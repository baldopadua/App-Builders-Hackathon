import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'screens/onboarding_screen.dart';

void main() {
  runApp(const TanggolinApp());
}

class TanggolinApp extends StatelessWidget {
  const TanggolinApp({super.key});

  @override
  Widget build(BuildContext context) {
    const brandPurple = Color(0xFF581C87);

    return MaterialApp(
      title: 'Tanggolin - Local AI Privacy Keyboard',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: brandPurple),
        useMaterial3: true,
        textTheme: GoogleFonts.plusJakartaSansTextTheme(
          ThemeData.light().textTheme,
        ),
      ),
      home: const OnboardingScreen(),
    );
  }
}
