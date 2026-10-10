package com.smartshop.backend.config;

import com.smartshop.backend.entity.*;
import com.smartshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedCategoriesAndProducts();
    }

    private void seedUsers() {
        if (!userRepository.existsByEmail("admin@smartshop.com")) {
            User admin = User.builder()
                    .fullName("SmartShop Admin")
                    .email("admin@smartshop.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phoneNumber("9876543210")
                    .role(Role.ROLE_ADMIN)
                    .status("ACTIVE")
                    .build();
            User savedAdmin = userRepository.save(admin);
            cartRepository.save(Cart.builder().user(savedAdmin).subtotal(0.0).deliveryFee(0.0).total(0.0).build());
        }

        if (!userRepository.existsByEmail("customer@smartshop.com")) {
            User customer = User.builder()
                    .fullName("Rahul Sharma")
                    .email("customer@smartshop.com")
                    .password(passwordEncoder.encode("customer123"))
                    .phoneNumber("9123456789")
                    .role(Role.ROLE_CUSTOMER)
                    .status("ACTIVE")
                    .build();
            User savedCustomer = userRepository.save(customer);
            cartRepository.save(Cart.builder().user(savedCustomer).subtotal(0.0).deliveryFee(0.0).total(0.0).build());
        }
    }

    private void seedCategoriesAndProducts() {
        if (productRepository.count() >= 17) {
            return;
        }

        Category electronics = categoryRepository.findByNameIgnoreCase("Electronics")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Electronics")
                        .description("Latest gadgets, smart wearables, audio, and personal tech")
                        .image("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500")
                        .build()));

        Category fashion = categoryRepository.findByNameIgnoreCase("Fashion")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Fashion")
                        .description("Modern Indian ethnic & western clothing, footwear, and accessories")
                        .image("https://images.unsplash.com/photo-1445205170230-053b83016050?w=500")
                        .build()));

        Category homeLiving = categoryRepository.findByNameIgnoreCase("Home & Living")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Home & Living")
                        .description("Premium home décor, kitchenware, and lifestyle products")
                        .image("https://images.unsplash.com/photo-1513694203232-719a280e022f?w=500")
                        .build()));

        Category beauty = categoryRepository.findByNameIgnoreCase("Beauty")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Beauty")
                        .description("Skincare, makeup, grooming, and personal care essentials")
                        .image("https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500")
                        .build()));

        Category sports = categoryRepository.findByNameIgnoreCase("Sports")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Sports")
                        .description("Fitness equipment, activewear, and outdoor gear")
                        .image("https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500")
                        .build()));

        Category books = categoryRepository.findByNameIgnoreCase("Books")
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name("Books")
                        .description("Bestsellers, educational guides, and literature")
                        .image("https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500")
                        .build()));

        List<Product> seedProducts = Arrays.asList(
                // Electronics
                Product.builder()
                        .name("Smart Watch Ultra Pro")
                        .description("1.96\" AMOLED Display, Bluetooth Calling, 100+ Sports Modes, IP68 Waterproof")
                        .price(3999.0)
                        .originalPrice(7999.0)
                        .discount(50)
                        .stock(25)
                        .brand("FitPulse")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500")
                        .rating(4.8)
                        .reviewCount(142)
                        .build(),

                Product.builder()
                        .name("Wireless Noise Cancelling Earbuds")
                        .description("Active Noise Cancellation (ANC 32dB), 30 Hours Playtime, Fast Type-C Charging")
                        .price(2499.0)
                        .originalPrice(4999.0)
                        .discount(50)
                        .stock(40)
                        .brand("SoundPro")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500")
                        .rating(4.6)
                        .reviewCount(98)
                        .build(),

                Product.builder()
                        .name("Portable Bluetooth Speaker 20W")
                        .description("Deep Bass Sound, RGB Ambient LED Lights, IPX7 Waterproof, 12-Hour Battery Backup")
                        .price(1899.0)
                        .originalPrice(3499.0)
                        .discount(45)
                        .stock(18)
                        .brand("SoundPro")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500")
                        .rating(4.5)
                        .reviewCount(76)
                        .build(),

                Product.builder()
                        .name("UltraSlim Laptop 15.6 inch")
                        .description("Intel Core i5 13th Gen, 16GB LPDDR5 RAM, 512GB NVMe SSD, Windows 11 Home")
                        .price(54990.0)
                        .originalPrice(69990.0)
                        .discount(21)
                        .stock(10)
                        .brand("TechCorp")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500")
                        .rating(4.9)
                        .reviewCount(210)
                        .build(),

                Product.builder()
                        .name("5G Smartphone 128GB")
                        .description("6.7\" FHD+ 120Hz Display, 64MP OIS Camera, 5000mAh Battery, 67W Fast Charger")
                        .price(18999.0)
                        .originalPrice(24999.0)
                        .discount(24)
                        .stock(15)
                        .brand("TechCorp")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500")
                        .rating(4.7)
                        .reviewCount(315)
                        .build(),

                Product.builder()
                        .name("DSLR Camera 24.1 MP")
                        .description("Dual Lens Kit (18-55mm & 55-250mm), Wi-Fi & NFC Enabled, 4K Video Recording")
                        .price(42999.0)
                        .originalPrice(52999.0)
                        .discount(18)
                        .stock(6)
                        .brand("LensCraft")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500")
                        .rating(4.9)
                        .reviewCount(85)
                        .build(),

                Product.builder()
                        .name("Over-Ear Wireless Headphones")
                        .description("Soft Memory Foam Cushions, Dual Connectivity, HD Voice Mic, 40H Battery")
                        .price(2999.0)
                        .originalPrice(5999.0)
                        .discount(50)
                        .stock(30)
                        .brand("SoundPro")
                        .category(electronics)
                        .image("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500")
                        .rating(4.6)
                        .reviewCount(112)
                        .build(),

                // Fashion
                Product.builder()
                        .name("Men's Casual Sneakers")
                        .description("Lightweight Breathable Mesh, Cushioned Sole, Trendy Streetwear Style")
                        .price(1499.0)
                        .originalPrice(2999.0)
                        .discount(50)
                        .stock(50)
                        .brand("UrbanFit")
                        .category(fashion)
                        .image("https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500")
                        .rating(4.4)
                        .reviewCount(64)
                        .build(),

                Product.builder()
                        .name("Waterproof Laptop Backpack 30L")
                        .description("USB Charging Port, Anti-Theft Hidden Pocket, Fits up to 17 inch Laptops")
                        .price(999.0)
                        .originalPrice(2499.0)
                        .discount(60)
                        .stock(45)
                        .brand("UrbanFit")
                        .category(fashion)
                        .image("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500")
                        .rating(4.7)
                        .reviewCount(180)
                        .build(),

                Product.builder()
                        .name("Classic Aviator Sunglasses")
                        .description("UV400 Protection Polarization, Stainless Steel Frame, Protective Hard Case Included")
                        .price(799.0)
                        .originalPrice(1999.0)
                        .discount(60)
                        .stock(35)
                        .brand("UrbanFit")
                        .category(fashion)
                        .image("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=500")
                        .rating(4.5)
                        .reviewCount(92)
                        .build(),

                // Home & Living
                Product.builder()
                        .name("Stainless Steel Electric Kettle 1.8L")
                        .description("Auto Cut-off Security, 1500W Fast Boiling, Cool Touch Handle")
                        .price(899.0)
                        .originalPrice(1799.0)
                        .discount(50)
                        .stock(28)
                        .brand("HomeCraft")
                        .category(homeLiving)
                        .image("https://images.unsplash.com/photo-1594212699903-ec8a3eca50f6?w=500")
                        .rating(4.6)
                        .reviewCount(140)
                        .build(),

                Product.builder()
                        .name("Aroma Oil Diffuser & Humidifier")
                        .description("Ultrasonic Silent Cool Mist, 7 Color Changing LED Mood Lights, 300ml Capacity")
                        .price(1199.0)
                        .originalPrice(2499.0)
                        .discount(52)
                        .stock(22)
                        .brand("HomeCraft")
                        .category(homeLiving)
                        .image("https://images.unsplash.com/photo-1602928321679-560bb453f190?w=500")
                        .rating(4.7)
                        .reviewCount(115)
                        .build(),

                // Beauty
                Product.builder()
                        .name("Vitamin C Face Serum 30ml")
                        .description("Brightening & Anti-Aging Formula with Hyaluronic Acid & Ferulic Acid")
                        .price(499.0)
                        .originalPrice(999.0)
                        .discount(50)
                        .stock(60)
                        .brand("GlowBeauty")
                        .category(beauty)
                        .image("https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500")
                        .rating(4.8)
                        .reviewCount(230)
                        .build(),

                Product.builder()
                        .name("Professional Hair Dryer 2000W")
                        .description("Ionic Conditioning Technology, 2 Speed & 3 Heat Settings, Cool Shot Button")
                        .price(1299.0)
                        .originalPrice(2599.0)
                        .discount(50)
                        .stock(30)
                        .brand("GlowBeauty")
                        .category(beauty)
                        .image("https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500")
                        .rating(4.6)
                        .reviewCount(88)
                        .build(),

                // Sports
                Product.builder()
                        .name("High Density Yoga Mat 6mm")
                        .description("Anti-Slip TPE Material, Free Carrying Strap Included, Moisture Resistant")
                        .price(699.0)
                        .originalPrice(1499.0)
                        .discount(53)
                        .stock(40)
                        .brand("ActiveSport")
                        .category(sports)
                        .image("https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=500")
                        .rating(4.7)
                        .reviewCount(175)
                        .build(),

                Product.builder()
                        .name("Adjustable Dumbbell Set 20kg")
                        .description("Cast Iron Weight Plates with Non-Slip Rubber Grip Handle & Chrome Locks")
                        .price(2999.0)
                        .originalPrice(4999.0)
                        .discount(40)
                        .stock(15)
                        .brand("ActiveSport")
                        .category(sports)
                        .image("https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=500")
                        .rating(4.9)
                        .reviewCount(145)
                        .build(),

                // Books
                Product.builder()
                        .name("Atomic Habits by James Clear")
                        .description("An Easy & Proven Way to Build Good Habits & Break Bad Ones (Paperback Edition)")
                        .price(399.0)
                        .originalPrice(799.0)
                        .discount(50)
                        .stock(80)
                        .brand("PageTurn")
                        .category(books)
                        .image("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500")
                        .rating(4.9)
                        .reviewCount(520)
                        .build()
        );

        productRepository.saveAll(seedProducts);
    }
}
