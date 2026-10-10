package com.smartshop.backend.service;

import com.smartshop.backend.dto.*;
import com.smartshop.backend.entity.*;
import com.smartshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ChatbotService – Intent-based chat engine that dispatches to existing SmartShop APIs.
 * Designed with pluggable architecture so an AI provider can be swapped in later.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ChatbotService {

    private final ProductService productService;
    private final OrderService orderService;
    private final CartService cartService;
    private final FAQRepository faqRepository;
    private final UserRepository userRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    // ─────────────────────── INTENTS ───────────────────────
    public enum Intent {
        PRODUCT_SEARCH, PRODUCT_RECOMMENDATION,
        ORDER_STATUS, ORDER_DETAILS, ORDER_LIST,
        CART_VIEW, CART_UPDATE,
        ORDER_CANCEL, RETURN_REQUEST, REFUND_QUERY,
        DELIVERY_QUERY, PAYMENT_QUERY,
        ACCOUNT_QUERY, CUSTOMER_CARE,
        GENERAL_FAQ, GREETING,
        UNKNOWN
    }

    private Intent detectIntent(String message) {
        String lower = message.toLowerCase();
        if (matches(lower, "\\bhi\\b|\\bhello\\b|\\bhey\\b|good morning|good evening|good afternoon|namaste|howdy")) return Intent.GREETING;
        if (matches(lower, "where is my order|track.*order|order.*status|shipment|has.*shipped|delivery status")) return Intent.ORDER_STATUS;
        if (matches(lower, "my orders|show.*orders|list.*orders|all orders|recent orders|past orders")) return Intent.ORDER_LIST;
        if (matches(lower, "ORD-|order number|order id")) return Intent.ORDER_DETAILS;
        if (matches(lower, "my cart|view cart|what.*in.*cart|cart items|cart total|show.*cart")) return Intent.CART_VIEW;
        if (matches(lower, "add.*cart|remove.*cart|update.*cart|change.*quantity|increase.*quantity")) return Intent.CART_UPDATE;
        if (matches(lower, "show me|find|search|looking for|recommend|suggest|under ₹|under rs|below ₹|need a|want a")) return Intent.PRODUCT_SEARCH;
        if (matches(lower, "laptop|phone|mobile|headphone|earphone|watch|tablet|camera|tv|speaker|keyboard|mouse|earbuds")) return Intent.PRODUCT_SEARCH;
        if (matches(lower, "return|exchange|replace|wrong product|damaged|defective")) return Intent.RETURN_REQUEST;
        if (matches(lower, "refund|money back|cashback")) return Intent.REFUND_QUERY;
        if (matches(lower, "cancel.*order|order.*cancel")) return Intent.ORDER_CANCEL;
        if (matches(lower, "delivery|shipping|ship|when.*arrive|how long|free delivery|delivery charge|dispatch")) return Intent.DELIVERY_QUERY;
        if (matches(lower, "payment|pay|upi|credit card|debit card|net banking|cod|cash on delivery|payment fail")) return Intent.PAYMENT_QUERY;
        if (matches(lower, "password|profile|address|phone number|logout|sign out|account|update.*profile")) return Intent.ACCOUNT_QUERY;
        if (matches(lower, "agent|human|support|customer care|contact|ticket|complaint|escalate|talk to")) return Intent.CUSTOMER_CARE;
        if (matches(lower, "faq|policy|terms|how does|how can|how do")) return Intent.GENERAL_FAQ;
        return Intent.UNKNOWN;
    }

    private boolean matches(String text, String pattern) {
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    // ─────────────────────── MAIN PROCESSOR ───────────────────────

    @Transactional
    public ChatMessageResponse processMessage(ChatMessageRequest request, String userEmail) {
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            return buildError("Please type a message.");
        }
        Intent intent = detectIntent(message);
        User user = (userEmail != null) ? userRepository.findByEmail(userEmail).orElse(null) : null;

        ChatConversation conversation = getOrCreateConversation(request, user);
        saveMessage(conversation, "USER", message, intent.name(), null);

        ChatMessageResponse response = switch (intent) {
            case GREETING              -> handleGreeting(user);
            case PRODUCT_SEARCH,
                 PRODUCT_RECOMMENDATION -> handleProductSearch(message);
            case ORDER_STATUS,
                 ORDER_DETAILS         -> handleOrderStatus(message, user);
            case ORDER_LIST            -> handleOrderList(user);
            case CART_VIEW             -> handleCartView(user);
            case CART_UPDATE           -> handleCartUpdate();
            case RETURN_REQUEST        -> handleReturn();
            case REFUND_QUERY          -> handleRefund();
            case ORDER_CANCEL          -> handleOrderCancel();
            case DELIVERY_QUERY        -> handleDelivery();
            case PAYMENT_QUERY         -> handlePayment();
            case ACCOUNT_QUERY         -> handleAccount(message);
            case CUSTOMER_CARE         -> handleCustomerCare();
            case GENERAL_FAQ           -> handleFAQ(message);
            default                    -> handleUnknown();
        };

        saveMessage(conversation, "BOT", response.getReply(), intent.name(), null);
        response.setConversationId(String.valueOf(conversation.getId()));
        return response;
    }

    // ─────────────────────── HANDLERS ───────────────────────

    private ChatMessageResponse handleGreeting(User user) {
        String name = (user != null) ? user.getFullName().split(" ")[0] : "there";
        return ChatMessageResponse.builder()
                .success(true)
                .reply("Hi " + name + "! 👋 Welcome to SmartShop!\n\nI'm your virtual assistant. How can I help you today?\n\n• 🔍 Find products\n• 📦 Track your order\n• 🛒 View your cart\n• 🚚 Delivery info\n• ↩️ Returns & refunds\n• 💳 Payment help")
                .intent(Intent.GREETING.name())
                .actions(Collections.emptyList())
                .build();
    }

    private ChatMessageResponse handleProductSearch(String message) {
        try {
            String keyword = extractProductKeyword(message);
            Double maxPrice = extractPrice(message);
            List<ProductDTO> all = productService.getAllProducts(keyword, null, null, null, maxPrice, "priceAsc");
            List<ProductDTO> products = all.stream().limit(6).collect(Collectors.toList());

            if (products.isEmpty()) {
                return ChatMessageResponse.builder()
                        .success(true)
                        .reply("I couldn't find products matching \"" + (keyword != null ? keyword : "your search") + "\". Try a different keyword or browse all products.")
                        .intent(Intent.PRODUCT_SEARCH.name())
                        .actions(List.of(Map.of("label", "Browse All Products", "url", "/products")))
                        .build();
            }
            String priceNote = (maxPrice != null) ? " under ₹" + maxPrice.intValue() : "";
            return ChatMessageResponse.builder()
                    .success(true)
                    .reply("Here are some " + (keyword != null ? keyword + "s" : "products") + priceNote + " for you! 🛍️")
                    .intent(Intent.PRODUCT_SEARCH.name())
                    .products(products)
                    .actions(List.of(Map.of("label", "View All Products", "url", "/products")))
                    .build();
        } catch (Exception e) {
            return ChatMessageResponse.builder()
                    .success(true)
                    .reply("Let me help you find products! Browse our full catalog.")
                    .intent(Intent.PRODUCT_SEARCH.name())
                    .actions(List.of(Map.of("label", "Browse Products", "url", "/products")))
                    .build();
        }
    }

    private ChatMessageResponse handleOrderStatus(String message, User user) {
        if (user == null) {
            return ChatMessageResponse.builder().success(true)
                    .reply("To check your order status, please log in first. 🔐")
                    .intent(Intent.ORDER_STATUS.name())
                    .actions(List.of(Map.of("label", "Login", "url", "/login"))).build();
        }
        String orderNumber = extractOrderNumber(message);
        if (orderNumber != null) {
            try {
                OrderDTO order = orderService.getOrderByOrderNumber(orderNumber);
                if (!order.getUserId().equals(user.getId())) {
                    return buildError("I couldn't find that order in your account.");
                }
                return ChatMessageResponse.builder().success(true)
                        .reply("Order **" + order.getOrderNumber() + "**:\n\n📦 Status: **" + order.getOrderStatus() + "**\n💰 Amount: ₹" + order.getTotalAmount() + "\n🗓️ Placed: " + fmt(order.getCreatedAt()))
                        .intent(Intent.ORDER_DETAILS.name())
                        .orders(List.of(order))
                        .actions(List.of(Map.of("label", "View Order", "url", "/orders"))).build();
            } catch (Exception e) {
                return buildError("I couldn't find order " + orderNumber + " in your account.");
            }
        }
        return ChatMessageResponse.builder().success(true)
                .reply("Please share your order number (e.g., ORD-20241015-1234), or I can show all your recent orders.")
                .intent(Intent.ORDER_STATUS.name())
                .actions(List.of(Map.of("label", "View My Orders", "url", "/orders"))).build();
    }

    private ChatMessageResponse handleOrderList(User user) {
        if (user == null) {
            return ChatMessageResponse.builder().success(true)
                    .reply("Please log in to view your orders. 🔐")
                    .intent(Intent.ORDER_LIST.name())
                    .actions(List.of(Map.of("label", "Login", "url", "/login"))).build();
        }
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(user.getId(), null);
            if (orders.isEmpty()) {
                return ChatMessageResponse.builder().success(true)
                        .reply("You haven't placed any orders yet. Start shopping! 🛍️")
                        .intent(Intent.ORDER_LIST.name())
                        .actions(List.of(Map.of("label", "Browse Products", "url", "/products"))).build();
            }
            List<OrderDTO> recent = orders.stream().limit(3).collect(Collectors.toList());
            return ChatMessageResponse.builder().success(true)
                    .reply("Here are your recent orders 📦")
                    .intent(Intent.ORDER_LIST.name())
                    .orders(recent)
                    .actions(List.of(Map.of("label", "View All Orders", "url", "/orders"))).build();
        } catch (Exception e) {
            return buildError("Couldn't fetch your orders. Please try again.");
        }
    }

    private ChatMessageResponse handleCartView(User user) {
        if (user == null) {
            return ChatMessageResponse.builder().success(true)
                    .reply("Please log in to view your cart. 🔐")
                    .intent(Intent.CART_VIEW.name())
                    .actions(List.of(Map.of("label", "Login", "url", "/login"))).build();
        }
        try {
            CartDTO cart = cartService.getCartByUserId(user.getId());
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                return ChatMessageResponse.builder().success(true)
                        .reply("Your cart is empty! 🛒\n\nBrowse our products to find something you like.")
                        .intent(Intent.CART_VIEW.name())
                        .actions(List.of(Map.of("label", "Browse Products", "url", "/products"))).build();
            }
            return ChatMessageResponse.builder().success(true)
                    .reply("Here's your cart 🛒\n\n**" + cart.getItems().size() + " item(s)** — Total: **₹" + cart.getTotal() + "**")
                    .intent(Intent.CART_VIEW.name())
                    .cart(cart)
                    .actions(List.of(
                            Map.of("label", "View Cart", "url", "/cart"),
                            Map.of("label", "Checkout", "url", "/checkout")
                    )).build();
        } catch (Exception e) {
            return ChatMessageResponse.builder().success(true)
                    .reply("Let me take you to your cart! 🛒")
                    .intent(Intent.CART_VIEW.name())
                    .actions(List.of(Map.of("label", "View Cart", "url", "/cart"))).build();
        }
    }

    private ChatMessageResponse handleCartUpdate() {
        return ChatMessageResponse.builder().success(true)
                .reply("To modify cart items, please visit the cart page where you can update quantities or remove items.")
                .intent(Intent.CART_UPDATE.name())
                .actions(List.of(Map.of("label", "Go to Cart", "url", "/cart"))).build();
    }

    private ChatMessageResponse handleReturn() {
        return ChatMessageResponse.builder().success(true)
                .reply("To return a product:\n\n1. Go to **My Orders**\n2. Open the order\n3. Click **Request Return**\n4. Select a reason and submit\n\n📌 Return window: **7 days** from delivery\n📌 Refund processed in **5–7 business days**")
                .intent(Intent.RETURN_REQUEST.name())
                .actions(List.of(
                        Map.of("label", "My Orders", "url", "/orders"),
                        Map.of("label", "Create Support Ticket", "url", "/support/create")
                )).build();
    }

    private ChatMessageResponse handleRefund() {
        return ChatMessageResponse.builder().success(true)
                .reply("Refund information:\n\n💳 **Timeline:** 5–7 business days after return approval\n🔄 **Method:** Original payment method\n✅ **Eligible:** Damaged, wrong, or defective products\n\nNeed specific help? Create a support ticket.")
                .intent(Intent.REFUND_QUERY.name())
                .actions(List.of(
                        Map.of("label", "My Orders", "url", "/orders"),
                        Map.of("label", "Create Support Ticket", "url", "/support/create")
                )).build();
    }

    private ChatMessageResponse handleOrderCancel() {
        return ChatMessageResponse.builder().success(true)
                .reply("To cancel an order:\n\n1. Go to **My Orders**\n2. Open the order\n3. Click **Cancel Order**\n\n⚠️ Only orders in **PROCESSING** status can be cancelled. Shipped orders require a return request.")
                .intent(Intent.ORDER_CANCEL.name())
                .actions(List.of(Map.of("label", "My Orders", "url", "/orders"))).build();
    }

    private ChatMessageResponse handleDelivery() {
        return ChatMessageResponse.builder().success(true)
                .reply("🚚 **SmartShop Delivery Information**\n\n⏱️ Standard Delivery: **3–7 business days**\n⚡ Express Delivery: **1–2 business days** (selected cities)\n🆓 Free Delivery: On orders above **₹499**\n📍 Delivery Area: **Pan India**\n\nTrack your order in real-time via My Orders.")
                .intent(Intent.DELIVERY_QUERY.name())
                .actions(List.of(Map.of("label", "Track My Order", "url", "/orders"))).build();
    }

    private ChatMessageResponse handlePayment() {
        return ChatMessageResponse.builder().success(true)
                .reply("💳 **SmartShop Payment Options**\n\n✅ Cash on Delivery (COD)\n✅ UPI (GPay, PhonePe, Paytm)\n✅ Credit / Debit Card\n✅ Net Banking\n\n🔒 All payments are **secure and encrypted**\n\n⚠️ Payment failed? Retry or use a different method. Auto-refunds for failed payments take 3–5 days.")
                .intent(Intent.PAYMENT_QUERY.name())
                .actions(List.of(Map.of("label", "Contact Support", "url", "/support/create"))).build();
    }

    private ChatMessageResponse handleAccount(String message) {
        String lower = message.toLowerCase();
        if (matches(lower, "password")) {
            return ChatMessageResponse.builder().success(true)
                    .reply("To change your password:\n\n1. Go to **My Profile**\n2. Click **Change Password**\n3. Enter current and new password\n\nForgot your password? Use the **Forgot Password** option on the login page.")
                    .intent(Intent.ACCOUNT_QUERY.name())
                    .actions(List.of(Map.of("label", "My Profile", "url", "/profile"))).build();
        }
        if (matches(lower, "address")) {
            return ChatMessageResponse.builder().success(true)
                    .reply("To update your address:\n\n1. Go to **My Profile**\n2. Click **Manage Addresses**\n3. Add, edit, or remove addresses")
                    .intent(Intent.ACCOUNT_QUERY.name())
                    .actions(List.of(Map.of("label", "My Profile", "url", "/profile"))).build();
        }
        if (matches(lower, "logout|sign out")) {
            return ChatMessageResponse.builder().success(true)
                    .reply("To logout, click the **Logout** button in the navigation bar or your profile menu.")
                    .intent(Intent.ACCOUNT_QUERY.name())
                    .actions(Collections.emptyList()).build();
        }
        return ChatMessageResponse.builder().success(true)
                .reply("Manage your account from **My Profile**:\n\n• Update name & phone\n• Change password\n• Manage addresses\n• View order history")
                .intent(Intent.ACCOUNT_QUERY.name())
                .actions(List.of(Map.of("label", "My Profile", "url", "/profile"))).build();
    }

    private ChatMessageResponse handleCustomerCare() {
        return ChatMessageResponse.builder().success(true)
                .reply("I'm connecting you with SmartShop Customer Care. 👩‍💼\n\nYou can:\n• Create a **Support Ticket** for detailed help\n• Visit our **Customer Care** page\n• Chat with a live support agent")
                .intent(Intent.CUSTOMER_CARE.name())
                .actions(List.of(
                        Map.of("label", "Create Support Ticket", "url", "/support/create"),
                        Map.of("label", "Customer Care Center", "url", "/customer-care")
                )).build();
    }

    private ChatMessageResponse handleFAQ(String message) {
        try {
            List<FAQ> faqs = faqRepository.findByStatusOrderBySortOrderAscCreatedAtAsc("ACTIVE");
            String lower = message.toLowerCase();
            Optional<FAQ> match = faqs.stream()
                    .filter(f -> lower.contains(f.getCategory().toLowerCase())
                            || f.getQuestion().toLowerCase().contains(lower.substring(0, Math.min(lower.length(), 15))))
                    .findFirst();
            if (match.isPresent()) {
                FAQ faq = match.get();
                return ChatMessageResponse.builder().success(true)
                        .reply("**" + faq.getQuestion() + "**\n\n" + faq.getAnswer())
                        .intent(Intent.GENERAL_FAQ.name())
                        .actions(List.of(Map.of("label", "More FAQs", "url", "/customer-care"))).build();
            }
        } catch (Exception ignored) {}
        return ChatMessageResponse.builder().success(true)
                .reply("Visit our Customer Care page for frequently asked questions.")
                .intent(Intent.GENERAL_FAQ.name())
                .actions(List.of(Map.of("label", "Customer Care & FAQs", "url", "/customer-care"))).build();
    }

    private ChatMessageResponse handleUnknown() {
        return ChatMessageResponse.builder().success(true)
                .reply("I'm sorry, I didn't quite understand that. 🤔\n\nYou can ask me about:\n• 🔍 **Products** — \"Show me smartwatches under ₹5000\"\n• 📦 **Orders** — \"Where is my order?\"\n• 🛒 **Cart** — \"What's in my cart?\"\n• 🚚 **Delivery** — \"How long does delivery take?\"\n• ↩️ **Returns** — \"How do I return a product?\"\n• 💳 **Payments** — \"What payment methods do you accept?\"")
                .intent(Intent.UNKNOWN.name())
                .actions(List.of(
                        Map.of("label", "Customer Care", "url", "/customer-care"),
                        Map.of("label", "Create Ticket", "url", "/support/create")
                )).build();
    }

    // ─────────────────────── HELPERS ───────────────────────

    private ChatConversation getOrCreateConversation(ChatMessageRequest request, User user) {
        Long convId = null;
        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            try { convId = Long.parseLong(request.getConversationId()); } catch (NumberFormatException ignored) {}
        }
        if (convId != null) {
            Optional<ChatConversation> existing = chatConversationRepository.findById(convId);
            if (existing.isPresent()) return existing.get();
        }
        ChatConversation conv = ChatConversation.builder()
                .user(user)
                .sessionId(request.getSessionId())
                .status("ACTIVE")
                .build();
        return chatConversationRepository.save(conv);
    }

    private void saveMessage(ChatConversation conversation, String sender, String message, String intent, String metadata) {
        com.smartshop.backend.entity.ChatMessage msg = com.smartshop.backend.entity.ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .message(message)
                .intent(intent)
                .metadata(metadata)
                .build();
        chatMessageRepository.save(msg);
        // Update conversation timestamp
        chatConversationRepository.save(conversation);
    }

    private String extractProductKeyword(String message) {
        String lower = message.toLowerCase();
        String[] products = {"smartwatch","watch","laptop","phone","mobile","headphone","earphone","earbuds",
                "tablet","camera","tv","television","speaker","keyboard","mouse","monitor","printer",
                "clothing","shoes","bags","books","toys","furniture","electronics"};
        for (String p : products) {
            if (lower.contains(p)) return p;
        }
        String[] triggers = {"show me ","find ","search for ","looking for ","need a ","want a ","recommend "};
        for (String t : triggers) {
            int idx = lower.indexOf(t);
            if (idx >= 0) {
                String rest = lower.substring(idx + t.length()).trim();
                if (rest.length() > 2) return rest.split("\\s+(under|below|cheap|for|with|that)")[0].trim();
            }
        }
        return null;
    }

    private Double extractPrice(String message) {
        java.util.regex.Matcher m = Pattern.compile("(?:under|below|less than|upto|up to|within)?\\s*[₹rRs.]*\\s*(\\d+[,\\d]*)",
                Pattern.CASE_INSENSITIVE).matcher(message);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replace(",", "")); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String extractOrderNumber(String message) {
        java.util.regex.Matcher m = Pattern.compile("(ORD-\\d{8}-\\d{4})", Pattern.CASE_INSENSITIVE).matcher(message);
        if (m.find()) return m.group(1).toUpperCase();
        return null;
    }

    private String fmt(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private ChatMessageResponse buildError(String msg) {
        return ChatMessageResponse.builder().success(false).reply(msg)
                .intent(Intent.UNKNOWN.name())
                .actions(List.of(Map.of("label", "Customer Care", "url", "/customer-care")))
                .build();
    }

    // ─────────────────────── CONVERSATION MANAGEMENT ───────────────────────

    public List<ChatConversationDTO> getConversationsForUser(Long userId) {
        return chatConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    public ChatConversationDTO getConversationById(Long id, Long userId) {
        ChatConversation conv = chatConversationRepository.findById(id)
                .orElseThrow(() -> new com.smartshop.backend.exception.ResourceNotFoundException("Conversation", "id", id));
        if (userId != null && conv.getUser() != null && !conv.getUser().getId().equals(userId)) {
            throw new com.smartshop.backend.exception.BadRequestException("Access denied");
        }
        List<com.smartshop.backend.entity.ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        ChatConversationDTO dto = toSummaryDTO(conv);
        dto.setMessages(messages.stream().map(this::toMsgDTO).collect(Collectors.toList()));
        return dto;
    }

    public List<ChatConversationDTO> getAllConversations() {
        return chatConversationRepository.findAllByOrderByUpdatedAtDesc()
                .stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteConversation(Long id, Long userId) {
        ChatConversation conv = chatConversationRepository.findById(id)
                .orElseThrow(() -> new com.smartshop.backend.exception.ResourceNotFoundException("Conversation", "id", id));
        if (userId != null && conv.getUser() != null && !conv.getUser().getId().equals(userId)) {
            throw new com.smartshop.backend.exception.BadRequestException("Access denied");
        }
        chatConversationRepository.delete(conv);
    }

    @Transactional
    public ChatConversationDTO adminJoinConversation(Long convId, String adminEmail) {
        ChatConversation conv = chatConversationRepository.findById(convId)
                .orElseThrow(() -> new com.smartshop.backend.exception.ResourceNotFoundException("Conversation", "id", convId));
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new com.smartshop.backend.exception.ResourceNotFoundException("User", "email", adminEmail));
        conv.setAssignedAdmin(admin);
        conv.setStatus("HUMAN_AGENT_ACTIVE");
        saveMessage(conv, "BOT", "You are now connected with a SmartShop support agent. 👨‍💼 Please wait while they review your conversation.", "CUSTOMER_CARE", null);
        chatConversationRepository.save(conv);
        return getConversationById(convId, null);
    }

    @Transactional
    public ChatConversationDTO adminSendMessage(Long convId, String message, String adminEmail) {
        ChatConversation conv = chatConversationRepository.findById(convId)
                .orElseThrow(() -> new com.smartshop.backend.exception.ResourceNotFoundException("Conversation", "id", convId));
        saveMessage(conv, "ADMIN", message, "CUSTOMER_CARE", null);
        return getConversationById(convId, null);
    }

    private ChatConversationDTO toSummaryDTO(ChatConversation c) {
        List<com.smartshop.backend.entity.ChatMessage> msgs = c.getMessages();
        String lastMsg = msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).getMessage();
        if (lastMsg.length() > 80) lastMsg = lastMsg.substring(0, 80) + "...";
        return ChatConversationDTO.builder()
                .id(c.getId())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .userFullName(c.getUser() != null ? c.getUser().getFullName() : "Guest")
                .userEmail(c.getUser() != null ? c.getUser().getEmail() : "")
                .sessionId(c.getSessionId())
                .status(c.getStatus())
                .assignedAdminId(c.getAssignedAdmin() != null ? c.getAssignedAdmin().getId() : null)
                .assignedAdminName(c.getAssignedAdmin() != null ? c.getAssignedAdmin().getFullName() : null)
                .lastMessage(lastMsg)
                .messageCount(msgs.size())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ChatMessageDTO toMsgDTO(com.smartshop.backend.entity.ChatMessage m) {
        return ChatMessageDTO.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .sender(m.getSender())
                .message(m.getMessage())
                .intent(m.getIntent())
                .metadata(m.getMetadata())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
