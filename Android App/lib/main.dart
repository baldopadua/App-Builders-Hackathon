import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'screens/splash_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      systemNavigationBarColor: Colors.white,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
  runApp(const TanggolinApp());
}

/// Shared Design System Tokens according to TANGGOLIN_MOBILE_UI_DESIGN_REVIEW.md
class AppTokens {
  static const Color brandPurple = Color(0xFF581C87);     // Primary action, deep purple
  static const Color brandAccent = Color(0xFF7C3AED);     // Focus, selected, link accent
  static const Color purpleLight = Color(0xFF6D28D9);     // Supporting purple
  static const Color surfaceWhite = Color(0xFFFFFFFF);    // Main utility surface
  static const Color surfaceTinted = Color(0xFFFAF5FF);   // App tinted surface
  static const Color lavenderSelected = Color(0xFFF3E8FF);// Selection / quiet grouping
  static const Color borderLavender = Color(0xFFE9D5FF);  // Decorative border
  static const Color borderNeutral = Color(0xFFE2E8F0);   // Input / card border
  static const Color textPrimary = Color(0xFF1E293B);     // Body & headings
  static const Color textSecondary = Color(0xFF64748B);   // Secondary & helpers (WCAG 4.76:1)
  static const Color brandGold = Color(0xFFFBB02B);       // Mascot accent gold
  static const Color amberBg = Color(0xFFFFFDF2);         // Amber warning bg
  static const Color amberBorder = Color(0xFFFDE68A);     // Amber warning border
  static const Color amberText = Color(0xFF78350F);       // Amber readable text
  static const Color redBg = Color(0xFFFFF8F8);           // Red warning bg
  static const Color redBorder = Color(0xFFFECDD3);       // Red warning border
  static const Color redText = Color(0xFF991B1B);         // Red readable text
}

class TanggolinApp extends StatelessWidget {
  const TanggolinApp({super.key});

  @override
  Widget build(BuildContext context) {
    final baseTextTheme = GoogleFonts.plusJakartaSansTextTheme(
      ThemeData.light().textTheme,
    );

    return MaterialApp(
      title: 'Tanggolin - Local AI Privacy Keyboard',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: AppTokens.surfaceWhite,
        colorScheme: ColorScheme.fromSeed(
          seedColor: AppTokens.brandPurple,
          primary: AppTokens.brandPurple,
          secondary: AppTokens.brandAccent,
          surface: AppTokens.surfaceWhite,
        ),
        textTheme: baseTextTheme.apply(
          bodyColor: AppTokens.textPrimary,
          displayColor: AppTokens.textPrimary,
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: AppTokens.surfaceWhite,
          surfaceTintColor: Colors.transparent,
          elevation: 0,
          scrolledUnderElevation: 0,
          centerTitle: false,
          iconTheme: IconThemeData(color: AppTokens.textPrimary),
          titleTextStyle: TextStyle(
            color: AppTokens.textPrimary,
            fontSize: 20,
            fontWeight: FontWeight.w700,
            letterSpacing: -0.3,
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTokens.brandPurple,
            foregroundColor: Colors.white,
            elevation: 0,
            minimumSize: const Size.fromHeight(50),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            textStyle: GoogleFonts.plusJakartaSans(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: AppTokens.surfaceWhite,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppTokens.borderNeutral),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppTokens.borderNeutral),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppTokens.brandAccent, width: 1.5),
          ),
          hintStyle: GoogleFonts.plusJakartaSans(
            color: AppTokens.textSecondary,
            fontSize: 14,
            fontWeight: FontWeight.w400,
          ),
        ),
      ),
      home: const SplashScreen(),
    );
  }
}
