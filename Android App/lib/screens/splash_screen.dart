import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/platform_bridge.dart';
import 'onboarding_screen.dart';
import 'practice_screen.dart';

/// Launch loading screen that presents the Tanggolin mascot SVG icon
/// spinning clockwise with smooth transition into the app.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _rotationController;
  late Animation<double> _rotationAnimation;
  late Animation<double> _scaleAnimation;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();

    // Controller runs for one deliberate full rotation cycle (2000ms)
    _rotationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2000),
    );

    _rotationAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _rotationController,
        curve: Curves.easeInOutCubic,
      ),
    );

    _scaleAnimation = Tween<double>(begin: 0.85, end: 1.0).animate(
      CurvedAnimation(
        parent: _rotationController,
        curve: const Interval(0.0, 0.35, curve: Curves.easeOutBack),
      ),
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _rotationController,
        curve: const Interval(0.0, 0.25, curve: Curves.easeIn),
      ),
    );

    _initializeApp();
  }

  Future<void> _initializeApp() async {
    bool checksDone = false;
    List<dynamic>? results;

    // Kick off platform checks in parallel with the rotation
    final platformChecks = Future.wait([
      PlatformBridge.isKeyboardEnabled(),
      PlatformBridge.isKeyboardSelected(),
    ]);

    platformChecks.then((res) {
      results = res;
      checksDone = true;
    }).catchError((_) {
      results = [false, false];
      checksDone = true;
    });

    // Animate at least one complete full 360° rotation
    await _rotationController.forward();

    // If platform checks are still running, complete subsequent full rotations
    while (!checksDone) {
      _rotationController.reset();
      await _rotationController.forward();
    }

    // Brief settling moment so the completed upright mascot registers before transition
    await Future.delayed(const Duration(milliseconds: 140));

    if (!mounted) return;

    final isEnabled = (results?[0] as bool?) ?? false;
    final isSelected = (results?[1] as bool?) ?? false;

    // If keyboard is already enabled and set as default, jump right to PracticeScreen
    final Widget destination = (isEnabled && isSelected)
        ? const PracticeScreen()
        : const OnboardingScreen();

    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) => destination,
        transitionsBuilder: (context, animation, secondaryAnimation, child) {
          return FadeTransition(
            opacity: CurvedAnimation(
              parent: animation,
              curve: Curves.easeInOut,
            ),
            child: child,
          );
        },
        transitionDuration: const Duration(milliseconds: 320),
      ),
    );
  }

  @override
  void dispose() {
    _rotationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const brandPurpleDark = Color(0xFF3B0764); // Rich deep violet
    const brandPurpleMid = Color(0xFF581C87);  // Signature Tanggolin purple
    const brandPurpleLight = Color(0xFF6D28D9);

    return Scaffold(
      backgroundColor: brandPurpleMid,
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              brandPurpleDark,
              brandPurpleMid,
              brandPurpleLight,
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(flex: 3),

              // High-Contrast Luminous Badge with Rotating Tanggolin Mascot
              Center(
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    // Outer ambient violet/lavender glow halo
                    Container(
                      width: 176,
                      height: 176,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white.withValues(alpha: 0.08),
                        boxShadow: [
                          BoxShadow(
                            color:
                                const Color(0xFFC084FC).withValues(alpha: 0.40),
                            blurRadius: 44,
                            spreadRadius: 8,
                          ),
                        ],
                      ),
                    ),

                    // Outer orbit accent ring rotating with the mascot
                    RotationTransition(
                      turns: _rotationAnimation,
                      child: Container(
                        width: 158,
                        height: 158,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(
                            color: Colors.white.withValues(alpha: 0.35),
                            width: 1.5,
                          ),
                        ),
                      ),
                    ),

                    // Luminous high-contrast White Emblem Badge
                    Container(
                      width: 134,
                      height: 134,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white,
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(alpha: 0.18),
                            blurRadius: 24,
                            offset: const Offset(0, 8),
                          ),
                          BoxShadow(
                            color:
                                const Color(0xFF7C3AED).withValues(alpha: 0.30),
                            blurRadius: 32,
                            spreadRadius: 2,
                          ),
                        ],
                      ),
                      padding: const EdgeInsets.all(18),
                      // Smooth clockwise rotation of the Tanggolin mascot
                      child: ScaleTransition(
                        scale: _scaleAnimation,
                        child: FadeTransition(
                          opacity: _fadeAnimation,
                          child: RotationTransition(
                            turns: _rotationAnimation,
                            child: Image.asset(
                              'assets/icons/tanggolin_icon.png',
                              fit: BoxFit.contain,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 36),

              // App Name
              Text(
                'TANGGOLIN',
                textAlign: TextAlign.center,
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 26,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                  letterSpacing: 3.2,
                ),
              ),

              const SizedBox(height: 8),

              // Subtext Tagline
              Text(
                'Local AI Privacy Keyboard',
                textAlign: TextAlign.center,
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w500,
                  color: const Color(0xFFE9D5FF),
                  letterSpacing: 0.4,
                ),
              ),

              const Spacer(flex: 3),

              // Discreet Bottom Loading Indicator
              Padding(
                padding: const EdgeInsets.only(bottom: 28),
                child: Column(
                  children: [
                    SizedBox(
                      width: 32,
                      height: 32,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.4,
                        valueColor: AlwaysStoppedAnimation<Color>(
                          Colors.white.withValues(alpha: 0.75),
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                    Text(
                      'Depensa sa bawat pindot...',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 12,
                        color: Colors.white.withValues(alpha: 0.65),
                        letterSpacing: 0.2,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
