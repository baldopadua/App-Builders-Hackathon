import 'package:flutter/material.dart';
import 'screens/onboarding_screen.dart';

void main() {
  runApp(const SecondLookApp());
}

class SecondLookApp extends StatelessWidget {
  const SecondLookApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SecondLook Keyboard',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const OnboardingScreen(),
    );
  }
}
