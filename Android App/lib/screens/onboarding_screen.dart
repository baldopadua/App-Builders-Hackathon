import 'package:flutter/material.dart';
import '../services/platform_bridge.dart';
import 'practice_screen.dart';

class OnboardingStepData {
  final String titleLine1;
  final String titleLine2;
  final String description;
  final IconData icon;

  const OnboardingStepData({
    required this.titleLine1,
    required this.titleLine2,
    required this.description,
    required this.icon,
  });
}

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen>
    with WidgetsBindingObserver {
  int _currentIndex = 0;
  bool _showActivation = false;

  bool _isMovingForward = true;

  // Activation checklist state
  bool _step1Completed = false; // Enable in Settings
  bool _step2Completed = false; // Switch Input Method
  bool _step3Completed = false; // Test Drive

  static const List<OnboardingStepData> _steps = [
    OnboardingStepData(
      titleLine1: 'Protect Your',
      titleLine2: 'Personal Information',
      description:
          'Accidental leaks happen in an instant. Tanggolin monitors your keystrokes and raises its guard before you hit send.',
      icon: Icons.shield_outlined,
    ),
    OnboardingStepData(
      titleLine1: 'Teach Me',
      titleLine2: "What's Private",
      description:
          "Save personal notes you never want exposed. Tanggolin's on-device AI detects context and meaning, not just exact keywords.",
      icon: Icons.lock_outline_rounded,
    ),
    OnboardingStepData(
      titleLine1: 'Curled Up',
      titleLine2: 'Zero Cloud.',
      description:
          'Like a pangolin, your data stays tightly sealed on your phone. Zero servers, no internet permission, and no keystrokes logged.',
      icon: Icons.cloud_off_rounded,
    ),
    OnboardingStepData(
      titleLine1: 'Activate',
      titleLine2: 'Tanggolin',
      description:
          'Complete 3 quick steps to set Tanggolin as your active keyboard shield.',
      icon: Icons.keyboard_alt_outlined,
    ),
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkKeyboardStatus();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkKeyboardStatus();
    }
  }

  Future<void> _checkKeyboardStatus() async {
    final isEnabled = await PlatformBridge.isKeyboardEnabled();
    final isSelected = await PlatformBridge.isKeyboardSelected();

    if (mounted) {
      setState(() {
        if (isEnabled) _step1Completed = true;
        if (isSelected) {
          _step1Completed = true;
          _step2Completed = true;
        }
      });
    }
  }

  void _onNextClicked() {
    if (_currentIndex < _steps.length - 1) {
      setState(() {
        _isMovingForward = true;
        _currentIndex++;
      });
    } else {
      setState(() {
        _showActivation = true;
      });
    }
  }

  void _onIndicatorTapped(int index) {
    if (index == _currentIndex) return;
    setState(() {
      _isMovingForward = index > _currentIndex;
      _currentIndex = index;
    });
  }

  // Handle Step 1: Enable in Settings
  Future<void> _handleStep1() async {
    await PlatformBridge.openInputMethodSettings();
    await Future.delayed(const Duration(milliseconds: 600));
    final enabled = await PlatformBridge.isKeyboardEnabled();
    if (mounted) {
      setState(() {
        _step1Completed = enabled || true; // Fallback so user is not blocked
      });
    }
  }

  // Handle Step 2: Switch Input Method
  Future<void> _handleStep2() async {
    if (!_step1Completed) {
      _showOrderWarning('Mangyaring paganahin muna ang Step 1 sa Settings.');
      return;
    }
    await PlatformBridge.showInputMethodPicker();
    await Future.delayed(const Duration(milliseconds: 600));
    final selected = await PlatformBridge.isKeyboardSelected();
    if (mounted) {
      setState(() {
        _step2Completed = selected || true;
      });
    }
  }

  // Handle Step 3: Test Drive (No redirect needed)
  void _handleStep3() {
    if (!_step2Completed) {
      _showOrderWarning('Piliin muna ang Tanggolin sa Step 2.');
      return;
    }
    setState(() {
      _step3Completed = true;
    });
  }

  void _showOrderWarning(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
        backgroundColor: const Color(0xFF6B21A8),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  void _finishOnboarding() {
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (context, animation, secondaryAnimation) =>
            const PracticeScreen(),
        transitionsBuilder: (context, animation, secondaryAnimation, child) {
          return FadeTransition(
            opacity: animation,
            child: child,
          );
        },
        transitionDuration: const Duration(milliseconds: 300),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    const brandPurple = Color(0xFF581C87); // Deep rich violet

    return Scaffold(
      backgroundColor: Colors.white,
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 350),
        switchInCurve: Curves.easeInOut,
        switchOutCurve: Curves.easeInOut,
        child: _showActivation
            ? _buildActivationScreen(brandPurple)
            : _buildContentSlides(brandPurple),
      ),
    );
  }

  /// Builds the in-place content screen with stationary hero and bottom card
  Widget _buildContentSlides(Color brandPurple) {
    final currentStep = _steps[_currentIndex];

    // Indicator track dimensions
    const double slotWidth = 26.0;
    const double slotGap = 6.0;
    const double slotHeight = 4.0;
    final double totalTrackWidth =
        (_steps.length * slotWidth) + ((_steps.length - 1) * slotGap);

    return Column(
      key: const ValueKey('onboarding_content_shell'),
      children: [
        // Top 60%: Stationary Purple Artwork Area with animated icon
        Expanded(
          flex: 6,
          child: Container(
            width: double.infinity,
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Color(0xFF4C1D95),
                  Color(0xFF5B21B6),
                  Color(0xFF6D28D9),
                ],
              ),
            ),
            child: Stack(
              children: [
                // Decorative Ambient Blobs / Rings
                Positioned(
                  top: -60,
                  right: -60,
                  child: Container(
                    width: 220,
                    height: 220,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.white.withValues(alpha: 0.06),
                    ),
                  ),
                ),
                Positioned(
                  bottom: 30,
                  left: -40,
                  child: Container(
                    width: 160,
                    height: 160,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.white.withValues(alpha: 0.04),
                    ),
                  ),
                ),

                // Center Icon with directional slide & scale AnimatedSwitcher
                Center(
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 320),
                    transitionBuilder: (child, animation) {
                      final isCurrent =
                          (child.key as ValueKey<int>?)?.value == _currentIndex;
                      final inOffset = Offset(_isMovingForward ? 0.4 : -0.4, 0);
                      final outOffset = Offset(_isMovingForward ? -0.4 : 0.4, 0);

                      final tween = Tween<Offset>(
                        begin: isCurrent ? inOffset : outOffset,
                        end: Offset.zero,
                      );

                      return FadeTransition(
                        opacity: animation,
                        child: SlideTransition(
                          position: tween.animate(
                            CurvedAnimation(
                              parent: animation,
                              curve: Curves.easeInOutCubic,
                            ),
                          ),
                          child: ScaleTransition(
                            scale: Tween<double>(begin: 0.85, end: 1.0).animate(
                              CurvedAnimation(
                                parent: animation,
                                curve: Curves.easeInOutCubic,
                              ),
                            ),
                            child: child,
                          ),
                        ),
                      );
                    },
                    child: Container(
                      key: ValueKey<int>(_currentIndex),
                      width: 110,
                      height: 110,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white.withValues(alpha: 0.12),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.25),
                          width: 2,
                        ),
                      ),
                      child: Icon(
                        currentStep.icon,
                        size: 54,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),

        // Bottom 40%: Stationary White Rounded Card with animated text & sliding indicator
        Expanded(
          flex: 4,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(28, 30, 28, 28),
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.only(
                topLeft: Radius.circular(32),
                topRight: Radius.circular(32),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Content area with smooth AnimatedSwitcher on step change
                Expanded(
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 320),
                    layoutBuilder: (currentChild, previousChildren) {
                      return Stack(
                        alignment: Alignment.topLeft,
                        children: <Widget>[
                          ...previousChildren,
                          ?currentChild,
                        ],
                      );
                    },
                    transitionBuilder: (child, animation) {
                      final isCurrent =
                          (child.key as ValueKey<int>?)?.value == _currentIndex;
                      // Outgoing text moves out to the left (-0.6)
                      // Incoming text moves in from the right (+0.6)
                      final inOffset = Offset(_isMovingForward ? 0.6 : -0.6, 0);
                      final outOffset = Offset(_isMovingForward ? -0.6 : 0.6, 0);

                      final tween = Tween<Offset>(
                        begin: isCurrent ? inOffset : outOffset,
                        end: Offset.zero,
                      );

                      return FadeTransition(
                        opacity: animation,
                        child: SlideTransition(
                          position: tween.animate(
                            CurvedAnimation(
                              parent: animation,
                              curve: Curves.easeInOutCubic,
                            ),
                          ),
                          child: child,
                        ),
                      );
                    },
                    child: SingleChildScrollView(
                      key: ValueKey<int>(_currentIndex),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Title Line 1
                          Text(
                            currentStep.titleLine1,
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF1F2937),
                              letterSpacing: -0.2,
                            ),
                          ),
                          // Title Line 2 (Brand Purple)
                          Text(
                            currentStep.titleLine2,
                            style: TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: brandPurple,
                              letterSpacing: -0.3,
                            ),
                          ),
                          const SizedBox(height: 12),
                          // Description
                          Text(
                            currentStep.description,
                            style: const TextStyle(
                              fontSize: 13.5,
                              height: 1.45,
                              color: Color(0xFF4B5563),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

                // Bottom Controls: Animated Sliding Indicator + "Get Started" Button
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    // Moving color page indicator with clickable slots
                    SizedBox(
                      width: totalTrackWidth,
                      height: 24, // Generous touch target height
                      child: Stack(
                        alignment: Alignment.centerLeft,
                        children: [
                          // Inactive slot backgrounds & tap detection
                          Row(
                            children: List.generate(_steps.length, (index) {
                              return GestureDetector(
                                behavior: HitTestBehavior.opaque,
                                onTap: () => _onIndicatorTapped(index),
                                child: Container(
                                  width: slotWidth,
                                  height: 24,
                                  margin: EdgeInsets.only(
                                    right: index < _steps.length - 1 ? slotGap : 0,
                                  ),
                                  alignment: Alignment.center,
                                  child: Container(
                                    width: slotWidth,
                                    height: slotHeight,
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFE5E7EB),
                                      borderRadius: BorderRadius.circular(2),
                                    ),
                                  ),
                                ),
                              );
                            }),
                          ),

                          // Animated Sliding Purple Indicator Bar
                          AnimatedPositioned(
                            duration: const Duration(milliseconds: 320),
                            curve: Curves.easeInOutCubic,
                            left: _currentIndex * (slotWidth + slotGap),
                            child: IgnorePointer(
                              child: Container(
                                width: slotWidth,
                                height: slotHeight,
                                decoration: BoxDecoration(
                                  color: brandPurple,
                                  borderRadius: BorderRadius.circular(2),
                                  boxShadow: [
                                    BoxShadow(
                                      color: brandPurple.withValues(alpha: 0.35),
                                      blurRadius: 4,
                                      offset: const Offset(0, 1),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                    // "Get Started" / "Next" Button
                    ElevatedButton(
                      onPressed: _onNextClicked,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: brandPurple,
                        foregroundColor: Colors.white,
                        elevation: 3,
                        shadowColor: brandPurple.withValues(alpha: 0.4),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 22,
                          vertical: 12,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                      ),
                      child: const Text(
                        'Get Started',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// Builds Slide 5: The Interactive Activation Checklist Screen
  Widget _buildActivationScreen(Color brandPurple) {
    final allDone = _step1Completed && _step2Completed && _step3Completed;

    return SafeArea(
      key: const ValueKey('activation_checklist_view'),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
        child: Column(
          children: [
            // Top Bar with Back Arrow to return to slides
            Align(
              alignment: Alignment.centerLeft,
              child: IconButton(
                icon: const Icon(Icons.arrow_back_ios_new, size: 20),
                color: brandPurple,
                onPressed: () {
                  setState(() {
                    _showActivation = false;
                  });
                },
              ),
            ),
            const SizedBox(height: 6),

            // Header Title
            Text(
              'Activate Tanggolin',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: brandPurple,
                letterSpacing: -0.4,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Complete the steps below in order to turn on your protective keyboard.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13.5,
                color: Color(0xFF6B7280),
              ),
            ),
            const SizedBox(height: 32),

            // Checklist Card 1: Enable in Settings
            _buildChecklistCard(
              title: 'Enable in Settings',
              subtitle: "Turn on Tanggolin under Android's Language & Input settings.",
              isCompleted: _step1Completed,
              isClickable: true,
              onTap: _handleStep1,
            ),
            const SizedBox(height: 18),

            // Checklist Card 2: Switch Input Method (Clickable after Step 1)
            _buildChecklistCard(
              title: 'Switch Input Method',
              subtitle: 'Select Tanggolin as your default on-screen keyboard',
              isCompleted: _step2Completed,
              isClickable: _step1Completed,
              onTap: _handleStep2,
            ),
            const SizedBox(height: 18),

            // Checklist Card 3: Test Drive (Clickable after Step 2, no redirect needed)
            _buildChecklistCard(
              title: 'Test Drive',
              subtitle: 'Verify detection with safe test inputs in our practice sandbox.',
              isCompleted: _step3Completed,
              isClickable: _step2Completed,
              onTap: _handleStep3,
            ),

            const Spacer(),

            // Bottom Action Button
            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton(
                onPressed: allDone ? _finishOnboarding : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: brandPurple,
                  disabledBackgroundColor: const Color(0xFFDDD6FE),
                  foregroundColor: Colors.white,
                  disabledForegroundColor: Colors.white,
                  elevation: allDone ? 3 : 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: Text(
                  allDone ? 'Start Using Tanggolin' : 'Next',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Builds each individual activation step card
  Widget _buildChecklistCard({
    required String title,
    required String subtitle,
    required bool isCompleted,
    required bool isClickable,
    required VoidCallback onTap,
  }) {
    const brandPurple = Color(0xFF581C87);
    final borderColor = isCompleted
        ? brandPurple
        : (isClickable ? const Color(0xFFC4B5FD) : const Color(0xFFE5E7EB));
    final cardBg = isCompleted
        ? const Color(0xFFFAF5FF)
        : (isClickable ? Colors.white : const Color(0xFFF9FAFB));

    return InkWell(
      onTap: isClickable
          ? onTap
          : () => _showOrderWarning('Mangyaring sundin ang tamang pagkakasunod-sunod.'),
      borderRadius: BorderRadius.circular(16),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        decoration: BoxDecoration(
          color: cardBg,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: borderColor,
            width: isCompleted ? 1.5 : 1.2,
          ),
          boxShadow: isClickable && !isCompleted
              ? [
                  BoxShadow(
                    color: brandPurple.withValues(alpha: 0.04),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ]
              : null,
        ),
        child: Row(
          children: [
            // Left content (Title and Subtitle)
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: 15.5,
                      fontWeight: FontWeight.bold,
                      color: isClickable ? brandPurple : const Color(0xFF9CA3AF),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12.5,
                      height: 1.35,
                      color: isClickable
                          ? const Color(0xFF4B5563)
                          : const Color(0xFF9CA3AF),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 14),

            // Trailing Status Indicator (Circle checkbox or checkmark)
            AnimatedContainer(
              duration: const Duration(milliseconds: 250),
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isCompleted ? brandPurple : Colors.transparent,
                border: Border.all(
                  color: isCompleted
                      ? brandPurple
                      : (isClickable
                          ? const Color(0xFF7C3AED)
                          : const Color(0xFFD1D5DB)),
                  width: 2,
                ),
              ),
              child: isCompleted
                  ? const Icon(
                      Icons.check,
                      size: 18,
                      color: Colors.white,
                    )
                  : null,
            ),
          ],
        ),
      ),
    );
  }
}
