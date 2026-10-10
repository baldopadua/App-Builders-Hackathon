import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../main.dart';
import '../services/platform_bridge.dart';
import 'about_screen.dart';
import 'practice_screen.dart';

class OnboardingStorySlide {
  final String headline;
  final String body;

  const OnboardingStorySlide({
    required this.headline,
    required this.body,
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

  // Real keyboard status flags - verified independently
  bool _isKeyboardEnabled = false;
  bool _isKeyboardSelected = false;
  bool _hasCompletedSafeTest = false;

  static const List<OnboardingStorySlide> _slides = [
    OnboardingStorySlide(
      headline: 'Catch private details before you share',
      body:
          'Tanggolin checks your draft for sensitive details and helps you mask them before sending.',
    ),
    OnboardingStorySlide(
      headline: 'Choose what stays private',
      body:
          'Add a private note so Tanggolin can flag related details and project names in your drafts.',
    ),
    OnboardingStorySlide(
      headline: 'Checks happen on your phone',
      body:
          'Draft checks run locally. Your saved private notes are encrypted strictly on this device.',
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
    try {
      final isEnabled = await PlatformBridge.isKeyboardEnabled();
      final isSelected = await PlatformBridge.isKeyboardSelected();
      if (mounted) {
        setState(() {
          _isKeyboardEnabled = isEnabled;
          _isKeyboardSelected = isSelected;
        });
      }
    } catch (_) {
      // Ignored
    }
  }

  void _onNext() {
    if (_currentIndex < _slides.length - 1) {
      setState(() {
        _currentIndex++;
      });
    } else {
      setState(() {
        _showActivation = true;
      });
    }
  }

  void _skipToActivation() {
    setState(() {
      _showActivation = true;
    });
  }

  Future<void> _handleEnableSettings() async {
    await PlatformBridge.openInputMethodSettings();
    Future.delayed(const Duration(milliseconds: 500), _checkKeyboardStatus);
  }

  Future<void> _handleChooseKeyboard() async {
    if (!_isKeyboardEnabled) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please enable Tanggolin in settings first.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }
    await PlatformBridge.showInputMethodPicker();
    Future.delayed(const Duration(milliseconds: 500), _checkKeyboardStatus);
  }

  void _startSafeTest() {
    setState(() {
      _hasCompletedSafeTest = true;
    });
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
        transitionDuration: const Duration(milliseconds: 250),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTokens.surfaceWhite,
      body: SafeArea(
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 280),
          child: _showActivation
              ? _buildActivationChecklistView()
              : _buildStorySlidesView(),
        ),
      ),
    );
  }

  /// Clean, modern story slide view - no artificial wireframe diagrams
  Widget _buildStorySlidesView() {
    final slide = _slides[_currentIndex];

    return Padding(
      key: const ValueKey('story_slides_view'),
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Top bar: Tanggolin logo + brand name, and skip action
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Image.asset(
                    'assets/icons/tanggolin_icon.png',
                    width: 28,
                    height: 28,
                    fit: BoxFit.contain,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Tanggolin',
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppTokens.brandPurple,
                      letterSpacing: -0.2,
                    ),
                  ),
                ],
              ),
              TextButton(
                onPressed: _skipToActivation,
                style: TextButton.styleFrom(
                  foregroundColor: AppTokens.brandAccent,
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                ),
                child: Text(
                  'Set up now',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),

          const Spacer(flex: 1),

          // Central Visual Hero
          Center(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 280),
              child: _buildHeroGraphic(_currentIndex),
            ),
          ),

          const Spacer(flex: 1),

          // Slide Text
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 220),
            child: Column(
              key: ValueKey<int>(_currentIndex),
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  slide.headline,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 26,
                    fontWeight: FontWeight.w700,
                    color: AppTokens.textPrimary,
                    letterSpacing: -0.4,
                    height: 1.25,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  slide.body,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 15,
                    fontWeight: FontWeight.w400,
                    color: AppTokens.textSecondary,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 32),

          // Footer: Step indicator + Next / Get started
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Clean step indicator
              Row(
                children: List.generate(_slides.length, (index) {
                  final isActive = index == _currentIndex;
                  return AnimatedContainer(
                    duration: const Duration(milliseconds: 220),
                    margin: const EdgeInsets.only(right: 6),
                    width: isActive ? 24 : 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: isActive
                          ? AppTokens.brandPurple
                          : const Color(0xFFE2E8F0),
                      borderRadius: BorderRadius.circular(4),
                    ),
                  );
                }),
              ),

              // Solid primary action button
              ElevatedButton(
                onPressed: _onNext,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTokens.brandPurple,
                  foregroundColor: Colors.white,
                  minimumSize: const Size(120, 50),
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: Text(
                  _currentIndex == _slides.length - 1
                      ? 'Get started'
                      : 'Next',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
        ],
      ),
    );
  }

  /// Clean, human-designed hero graphics for the slides
  Widget _buildHeroGraphic(int index) {
    switch (index) {
      case 0:
        return _buildHeroSlide1();
      case 1:
        return _buildHeroSlide2();
      case 2:
      default:
        return _buildHeroSlide3();
    }
  }

  Widget _buildHeroSlide1() {
    return Container(
      key: const ValueKey('hero_1'),
      width: 240,
      height: 240,
      decoration: BoxDecoration(
        color: AppTokens.surfaceTinted,
        shape: BoxShape.circle,
        border: Border.all(color: AppTokens.borderLavender, width: 1.5),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              'assets/icons/tanggolin_icon.png',
              width: 90,
              height: 90,
              fit: BoxFit.contain,
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTokens.borderNeutral),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.04),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.shield, size: 16, color: Color(0xFFD97706)),
                  const SizedBox(width: 8),
                  Text(
                    'Sensitive text masked',
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w600,
                      color: AppTokens.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroSlide2() {
    return Container(
      key: const ValueKey('hero_2'),
      width: 240,
      height: 240,
      decoration: BoxDecoration(
        color: AppTokens.surfaceTinted,
        shape: BoxShape.circle,
        border: Border.all(color: AppTokens.borderLavender, width: 1.5),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: const BoxDecoration(
                color: Colors.white,
                shape: BoxShape.circle,
              ),
              child: const Center(
                child: Icon(
                  Icons.bookmark_outline_rounded,
                  size: 40,
                  color: AppTokens.brandAccent,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTokens.borderNeutral),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.04),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Text(
                'Private Vault • Encrypted',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: AppTokens.brandPurple,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroSlide3() {
    return Container(
      key: const ValueKey('hero_3'),
      width: 240,
      height: 240,
      decoration: BoxDecoration(
        color: AppTokens.surfaceTinted,
        shape: BoxShape.circle,
        border: Border.all(color: AppTokens.borderLavender, width: 1.5),
      ),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              'assets/icons/tanggolin_icon.png',
              width: 90,
              height: 90,
              fit: BoxFit.contain,
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTokens.borderNeutral),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.04),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.lock_outline_rounded,
                      size: 15, color: AppTokens.brandPurple),
                  const SizedBox(width: 6),
                  Text(
                    '100% Offline • Zero cloud',
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w600,
                      color: AppTokens.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Clean, 3-step setup options page - BUG FIXED: each step has independent selection
  Widget _buildActivationChecklistView() {
    return Padding(
      key: const ValueKey('activation_view'),
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Back button
          InkWell(
            onTap: () {
              setState(() {
                _showActivation = false;
              });
            },
            borderRadius: BorderRadius.circular(8),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 2),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.arrow_back_ios_new,
                    size: 16,
                    color: AppTokens.textPrimary,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    'Back',
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: AppTokens.textPrimary,
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          Text(
            'Set up Tanggolin',
            style: GoogleFonts.plusJakartaSans(
              fontSize: 24,
              fontWeight: FontWeight.w700,
              color: AppTokens.textPrimary,
              letterSpacing: -0.4,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Complete setup to start protecting what you type.',
            style: GoogleFonts.plusJakartaSans(
              fontSize: 15,
              color: AppTokens.textSecondary,
              fontWeight: FontWeight.w400,
            ),
          ),

          const SizedBox(height: 24),

          // Option Box 1: Enable the keyboard
          _buildSetupOptionBox(
            stepNumber: '1',
            title: 'Enable the keyboard',
            subtitle: 'Turn on Tanggolin in Android keyboard settings.',
            isCompleted: _isKeyboardEnabled,
            isCurrent: !_isKeyboardEnabled,
            actionLabel: 'Enable',
            onTap: _handleEnableSettings,
          ),

          const SizedBox(height: 14),

          // Option Box 2: Choose Tanggolin (THE MIDDLE BOX)
          // BUG FIX: When this is selected, Option Box 3 does NOT get selected!
          _buildSetupOptionBox(
            stepNumber: '2',
            title: 'Choose Tanggolin',
            subtitle: _isKeyboardEnabled
                ? 'Select Tanggolin as your active keyboard.'
                : 'Complete step 1 to unlock this step.',
            isCompleted: _isKeyboardSelected,
            isCurrent: _isKeyboardEnabled && !_isKeyboardSelected,
            actionLabel: 'Select',
            onTap: _handleChooseKeyboard,
          ),

          const SizedBox(height: 14),

          // Option Box 3: Try a safe example
          // BUG FIX: isCompleted is strictly based on _hasCompletedSafeTest, NOT Step 2!
          _buildSetupOptionBox(
            stepNumber: '3',
            title: 'Try a safe example',
            subtitle: 'See detection and masking with sample text.',
            isCompleted: _hasCompletedSafeTest,
            isCurrent: _isKeyboardEnabled && _isKeyboardSelected,
            actionLabel: 'Test now',
            onTap: _startSafeTest,
          ),

          const Spacer(),

          // Dynamic primary button based on current pending step
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              onPressed: () {
                if (!_isKeyboardEnabled) {
                  _handleEnableSettings();
                } else if (!_isKeyboardSelected) {
                  _handleChooseKeyboard();
                } else {
                  _startSafeTest();
                }
              },
              child: Text(
                !_isKeyboardEnabled
                    ? '1. Open keyboard settings'
                    : (!_isKeyboardSelected
                        ? '2. Choose Tanggolin keyboard'
                        : '3. Try protection now'),
              ),
            ),
          ),

          const SizedBox(height: 14),

          Center(
            child: GestureDetector(
              onTap: () => AboutScreen.showTermsAndConditionsSheet(context),
              child: Text(
                'Terms & conditions',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                  color: AppTokens.brandAccent,
                  decoration: TextDecoration.underline,
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }

  /// Individual clean Setup Option Box
  Widget _buildSetupOptionBox({
    required String stepNumber,
    required String title,
    required String subtitle,
    required bool isCompleted,
    required bool isCurrent,
    required String actionLabel,
    required VoidCallback onTap,
  }) {
    final borderColor = isCompleted
        ? AppTokens.brandPurple
        : (isCurrent ? AppTokens.brandAccent : AppTokens.borderNeutral);

    final bgColor = isCompleted
        ? AppTokens.surfaceTinted
        : (isCurrent ? Colors.white : const Color(0xFFF8FAFC));

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: borderColor, width: isCompleted || isCurrent ? 1.5 : 1.0),
        ),
        child: Row(
          children: [
            // Number or Check Icon
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isCompleted
                    ? AppTokens.brandPurple
                    : (isCurrent ? AppTokens.lavenderSelected : const Color(0xFFE2E8F0)),
              ),
              child: Center(
                child: isCompleted
                    ? const Icon(Icons.check, size: 18, color: Colors.white)
                    : Text(
                        stepNumber,
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: isCurrent ? AppTokens.brandPurple : const Color(0xFF64748B),
                        ),
                      ),
              ),
            ),

            const SizedBox(width: 14),

            // Content
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 15.5,
                      fontWeight: FontWeight.w700,
                      color: AppTokens.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w400,
                      color: AppTokens.textSecondary,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(width: 10),

            // Trailing action or status
            if (isCompleted)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppTokens.lavenderSelected,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.check, size: 13, color: AppTokens.brandPurple),
                    const SizedBox(width: 4),
                    Text(
                      'Done',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: AppTokens.brandPurple,
                      ),
                    ),
                  ],
                ),
              )
            else if (isCurrent)
              OutlinedButton(
                onPressed: onTap,
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(0, 34),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
                  side: const BorderSide(color: AppTokens.brandAccent, width: 1.2),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: Text(
                  actionLabel,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: AppTokens.brandAccent,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
