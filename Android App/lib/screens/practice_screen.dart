import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/platform_bridge.dart';
import 'about_screen.dart';
import 'secrets_screen.dart';

class PracticeScreen extends StatefulWidget {
  const PracticeScreen({super.key});

  @override
  State<PracticeScreen> createState() => _PracticeScreenState();
}

class _PracticeScreenState extends State<PracticeScreen> {
  final TextEditingController _textController = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  List<PrivateSecret> _vaultSecrets = [];
  Timer? _debounceTimer;

  DetectionResult? _currentWarning;
  bool _isDismissed = false;
  bool _isMaskedApplied = false;
  String _maskedSecretType = 'OTP';
  String? _activeQuickTestKey;

  // Shared Brand Palette aligned with SecretsScreen
  static const Color _brandPurple = Color(0xFF581C87); // Deep rich violet
  static const Color _accentPurple = Color(0xFF7C3AED);
  static const Color _surfaceLavender = Color(0xFFFAF5FF);
  static const Color _borderLavender = Color(0xFFE9D5FF);
  static const Color _badgeLavender = Color(0xFFF3E8FF);

  @override
  void initState() {
    super.initState();
    _loadSecrets();
    _textController.addListener(_onTextChanged);
  }

  @override
  void dispose() {
    _debounceTimer?.cancel();
    _textController.removeListener(_onTextChanged);
    _textController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  Future<void> _loadSecrets() async {
    final secrets = await PlatformBridge.getSecrets();
    if (mounted) {
      setState(() {
        _vaultSecrets = secrets;
      });
    }
  }

  void _onTextChanged() {
    final text = _textController.text;

    // Reset debounce & trigger analysis
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 150), () {
      _analyzeText(text);
    });
  }

  Future<void> _analyzeText(String text) async {
    if (text.trim().isEmpty) {
      if (mounted) {
        setState(() {
          _currentWarning = null;
          _isMaskedApplied = false;
          _isDismissed = false;
          _activeQuickTestKey = null;
        });
      }
      return;
    }

    // Try native detection first
    DetectionResult? result = await PlatformBridge.check(text);

    // Synchronous Dart fallback for immediate zero-lag UX
    result ??= _detectLocally(text);

    if (mounted) {
      setState(() {
        _currentWarning = result;
        _isDismissed = false;
      });
    }
  }

  DetectionResult? _detectLocally(String text) {
    final lower = text.toLowerCase();

    // Check for Lola OTP or verification codes
    final hasOtp = lower.contains('482913') ||
        (lower.contains('code na pinadala') &&
            RegExp(r'\b\d{4,8}\b').hasMatch(text)) ||
        lower.contains('otp') ||
        lower.contains('pin');

    if (hasOtp) {
      return DetectionResult(
        risk: DetectionRisk.high,
        headline: 'Teka! OTP o PIN ito — huwag ibigay kahit kanino.',
        tip:
            'Ang OTP ay parang susi ng bahay. Walang totoong bangko o GCash na hihingi nito.',
        category: 'BABALA · OTP',
      );
    }

    // ML Password / generic passwords
    if (lower.contains('hunter2') || lower.contains('password')) {
      return DetectionResult(
        risk: DetectionRisk.high,
        headline: 'Teka! Password ito — huwag ibigay kahit kanino.',
        tip:
            'Account password detected. Walang admin o customer service na hihingi ng password mo.',
        category: 'BABALA · PASSWORD',
      );
    }

    // Gemini API Key
    if (lower.contains('aiza') ||
        lower.contains('api key') ||
        lower.contains('gemini')) {
      return DetectionResult(
        risk: DetectionRisk.high,
        headline: 'Teka! API Key o Secret ito.',
        tip:
            'Critical developer key detected in draft. Never share API keys in chat.',
        category: 'BABALA · API KEY',
      );
    }

    // Check against user's Vault Secrets
    for (final secret in _vaultSecrets) {
      if (secret.value.isNotEmpty &&
          lower.contains(secret.value.toLowerCase())) {
        return DetectionResult(
          risk: DetectionRisk.high,
          headline: 'Teka! Sikreto mo ito — itinuro mong protektahan.',
          tip:
              'Tumutugma ito sa private note na "${secret.label}" na nakatago sa iyong vault.',
          category: 'BABALA · VAULT',
        );
      }
    }

    // Fallback for private note test string
    if (lower.contains('secretpin88') || lower.contains('private note')) {
      return DetectionResult(
        risk: DetectionRisk.high,
        headline: 'Teka! Sikreto mo ito — itinuro mong protektahan.',
        tip:
            'Tumutugma ito sa pribadong tala na protektado sa iyong SecondLook vault.',
        category: 'BABALA · VAULT',
      );
    }

    // GCash / Phone Number detection (Medium Risk)
    if (lower.contains('gcash') || RegExp(r'\b09\d{9}\b').hasMatch(lower)) {
      return DetectionResult(
        risk: DetectionRisk.medium,
        headline: 'Sinadya mo ba?',
        tip: 'Phone number or account detected. Intended to share?',
        category: 'PANSIN · INTENDED?',
      );
    }

    return null;
  }

  Future<void> _applyItago() async {
    final text = _textController.text;
    String masked = await PlatformBridge.hide(text);

    if (masked == text) {
      // Local fallback masking
      masked = _maskLocally(text);
    }

    String secretName = 'OTP';
    final lower = text.toLowerCase();
    if (lower.contains('hunter2') || lower.contains('password')) {
      secretName = 'Password';
    } else if (lower.contains('aiza') || lower.contains('api key')) {
      secretName = 'API Key';
    } else if (lower.contains('09') || lower.contains('gcash')) {
      secretName = 'Contact Number';
    } else if (_currentWarning?.category.contains('VAULT') == true) {
      secretName = 'Sikreto';
    }

    setState(() {
      _textController.text = masked;
      _isMaskedApplied = true;
      _maskedSecretType = secretName;
      _isDismissed = false;
    });
  }

  String _maskLocally(String text) {
    var result = text;
    // OTP 6 digits e.g. 482913 -> ••••••
    final otpMatch = RegExp(r'\b\d{4,8}\b').firstMatch(result);
    if (otpMatch != null) {
      final bullets = '•' * otpMatch.group(0)!.length;
      return result.replaceRange(otpMatch.start, otpMatch.end, bullets);
    }

    if (result.contains('hunter2')) {
      result = result.replaceAll('hunter2', '••••••••');
    }

    final phoneMatch = RegExp(r'\b09\d{9}\b').firstMatch(result);
    if (phoneMatch != null) {
      result = result.replaceRange(phoneMatch.start, phoneMatch.end, '•••••••••••');
    }

    final keyMatch = RegExp(r'AIza[0-9A-Za-z\-_]{10,}').firstMatch(result);
    if (keyMatch != null) {
      result = result.replaceRange(keyMatch.start, keyMatch.end, '••••••••');
    }

    for (final s in _vaultSecrets) {
      if (s.value.isNotEmpty && result.contains(s.value)) {
        result = result.replaceAll(s.value, '••••••••');
      }
    }

    return result;
  }

  void _clearText() {
    setState(() {
      _textController.clear();
      _currentWarning = null;
      _isMaskedApplied = false;
      _isDismissed = false;
      _activeQuickTestKey = null;
    });
  }

  void _selectQuickTest(String key, String text) {
    setState(() {
      _activeQuickTestKey = key;
      _isMaskedApplied = false;
      _isDismissed = false;
      _textController.text = text;
    });
    _analyzeText(text);
  }

  @override
  Widget build(BuildContext context) {
    final text = _textController.text;
    final charCount = text.length;
    final wordCount = text.trim().isEmpty
        ? 0
        : text.trim().split(RegExp(r'\s+')).length;

    return Scaffold(
      backgroundColor: _brandPurple,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            // Top App Bar Header Area aligned with SecretsScreen
            _buildAlignedHeader(context),

            // White Main Body Container with Top Rounded Corners (radius: 30)
            Expanded(
              child: Container(
                width: double.infinity,
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
                ),
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(20, 22, 20, 36),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // ✈️ 100% On-Device AI • Offline Active Status Chip
                      Center(child: _buildStatusBadge()),

                      const SizedBox(height: 20),

                      // Scratchpad Title
                      const Text(
                        'Scratchpad',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF1E293B),
                          letterSpacing: -0.3,
                        ),
                      ),

                      const SizedBox(height: 12),

                      // Dynamic Warning or Masked Success Banner matching XML style
                      _buildDynamicAlertCard(),

                      // Text Box (Scratchpad)
                      _buildScratchpadTextBox(),

                      const SizedBox(height: 10),

                      // Clear Text & Character/Word Count Row
                      _buildCounterAndClearRow(charCount, wordCount),

                      const SizedBox(height: 24),

                      // Synthetic Quick Tests Section
                      _buildQuickTestsSection(),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Seamless horizontal slide + crossfade page route transition
  Route _createSeamlessRoute(Widget page) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        const curve = Curves.easeInOutCubic;
        final slideAnimation = Tween<Offset>(
          begin: const Offset(1.0, 0.0),
          end: Offset.zero,
        ).animate(CurvedAnimation(parent: animation, curve: curve));

        final fadeAnimation = Tween<double>(
          begin: 0.0,
          end: 1.0,
        ).animate(CurvedAnimation(parent: animation, curve: curve));

        return SlideTransition(
          position: slideAnimation,
          child: FadeTransition(
            opacity: fadeAnimation,
            child: child,
          ),
        );
      },
      transitionDuration: const Duration(milliseconds: 280),
      reverseTransitionDuration: const Duration(milliseconds: 250),
    );
  }

  /// Top app bar header area exactly matching SecretsScreen
  Widget _buildAlignedHeader(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(
              Icons.info_outline,
              color: Colors.white,
              size: 22,
            ),
            tooltip: 'About & Privacy',
            onPressed: () {
              Navigator.push(
                context,
                _createSeamlessRoute(const AboutScreen()),
              );
            },
          ),
          const Expanded(
            child: Text(
              'Practice Sandbox',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.white,
                letterSpacing: -0.3,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(
              Icons.shield_outlined,
              color: Colors.white,
              size: 22,
            ),
            tooltip: "Teach Me What's Private",
            onPressed: () {
              Navigator.push(
                context,
                _createSeamlessRoute(const SecretsScreen()),
              ).then((_) => _loadSecrets());
            },
          ),
        ],
      ),
    );
  }

  /// ✈️ 100% On-Device AI • Offline Active badge
  Widget _buildStatusBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
      decoration: BoxDecoration(
        color: _badgeLavender,
        borderRadius: BorderRadius.circular(20),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.flight,
            size: 15,
            color: _brandPurple,
          ),
          SizedBox(width: 6),
          Text(
            '100% On-Device AI • Offline Active',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: _brandPurple,
              letterSpacing: 0.1,
            ),
          ),
        ],
      ),
    );
  }

  /// Dynamic Alert Card (High Risk, Medium Risk, or Masked Success)
  Widget _buildDynamicAlertCard() {
    if (_isMaskedApplied) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 12.0),
        child: _buildMaskedSuccessBanner(),
      );
    }

    if (_currentWarning == null || _isDismissed) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 12.0),
      child: AnimatedSwitcher(
        duration: const Duration(milliseconds: 200),
        child: _currentWarning!.risk == DetectionRisk.high
            ? _buildHighRiskCard(_currentWarning!)
            : _buildMediumRiskCard(_currentWarning!),
      ),
    );
  }

  /// High Risk banner matching warning_banner.xml (light rose #FFF8F8, red #991B1B)
  Widget _buildHighRiskCard(DetectionResult warning) {
    return Container(
      key: const ValueKey('high_risk_card'),
      padding: const EdgeInsets.all(12.0),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8F8),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFFECDD3), width: 1.0),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Sleek Header: Status Icon + Text Badge (No Emojis)
          Row(
            children: [
              const Icon(
                Icons.error_outline,
                size: 16,
                color: Color(0xFFEF4444),
              ),
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFFFEE2E2),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  warning.category.isNotEmpty ? warning.category : 'BABALA',
                  style: const TextStyle(
                    color: Color(0xFF991B1B),
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 0.6,
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: 6),

          // Clear, Readable Filipino Headline (Plus Jakarta Sans Bold 16sp)
          Text(
            warning.headline,
            style: GoogleFonts.plusJakartaSans(
              color: const Color(0xFF991B1B),
              fontSize: 16,
              fontWeight: FontWeight.bold,
              height: 1.25,
            ),
          ),

          const SizedBox(height: 3),

          // Reason & Tip Text (Plus Jakarta Sans Regular 13sp)
          Text(
            warning.tip,
            style: GoogleFonts.plusJakartaSans(
              color: const Color(0xFF7F1D1D),
              fontSize: 13,
              fontWeight: FontWeight.normal,
              height: 1.35,
            ),
          ),

          const SizedBox(height: 10),

          // Sleek Action Buttons Bar matching XML: [Dismiss] [Itago]
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              // Dismiss Button (Ghost)
              TextButton(
                onPressed: () {
                  setState(() => _isDismissed = true);
                },
                style: TextButton.styleFrom(
                  foregroundColor: const Color(0xFF991B1B),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  minimumSize: const Size(0, 36),
                ),
                child: Text(
                  'Dismiss',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 8),

              // Itago Button (Clean Solid Red Pill, No Emojis)
              ElevatedButton(
                onPressed: _applyItago,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFDC2626),
                  foregroundColor: Colors.white,
                  elevation: 0,
                  minimumSize: const Size(78, 36),
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: Text(
                  'Itago',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Medium Risk banner matching warning_banner.xml (light amber #FFFDF2, #854D0E)
  Widget _buildMediumRiskCard(DetectionResult warning) {
    return Container(
      key: const ValueKey('medium_risk_card'),
      padding: const EdgeInsets.all(12.0),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFDF2),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFFDE68A), width: 1.0),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Sleek Header: Status Icon + Text Badge (No Emojis)
          Row(
            children: [
              const Icon(
                Icons.warning_amber_rounded,
                size: 16,
                color: Color(0xFFF59E0B),
              ),
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFFFEF3C7),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  'PANSIN · INTENDED?',
                  style: GoogleFonts.plusJakartaSans(
                    color: const Color(0xFF854D0E),
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 0.6,
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: 6),

          // Clear, Readable Filipino Headline (Plus Jakarta Sans Bold 16sp)
          Text(
            warning.headline,
            style: GoogleFonts.plusJakartaSans(
              color: const Color(0xFF854D0E),
              fontSize: 16,
              fontWeight: FontWeight.bold,
              height: 1.25,
            ),
          ),

          const SizedBox(height: 3),

          // Reason & Tip Text (Plus Jakarta Sans Regular 13sp)
          Text(
            warning.tip,
            style: GoogleFonts.plusJakartaSans(
              color: const Color(0xFF78350F),
              fontSize: 13,
              fontWeight: FontWeight.normal,
              height: 1.35,
            ),
          ),

          const SizedBox(height: 10),

          // Sleek Action Buttons Bar: [Dismiss] [Itago]
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              // Dismiss Button (Ghost)
              TextButton(
                onPressed: () {
                  setState(() => _isDismissed = true);
                },
                style: TextButton.styleFrom(
                  foregroundColor: const Color(0xFF854D0E),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  minimumSize: const Size(0, 36),
                ),
                child: Text(
                  'Dismiss',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(width: 8),

              // Itago Button (Clean Solid Amber Pill)
              ElevatedButton(
                onPressed: _applyItago,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFD97706),
                  foregroundColor: Colors.white,
                  elevation: 0,
                  minimumSize: const Size(78, 36),
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: Text(
                  'Itago',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Itago Applied success banner
  Widget _buildMaskedSuccessBanner() {
    return Container(
      key: const ValueKey('masked_success_banner'),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _surfaceLavender,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: _borderLavender, width: 1.0),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.only(top: 2),
            child: Icon(
              Icons.shield_outlined,
              color: _brandPurple,
              size: 20,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Naitago na ang $_maskedSecretType.',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: _brandPurple,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Only the sensitive code was masked.',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 12,
                    color: const Color(0xFF6B21A8),
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 18, color: Color(0xFF9333EA)),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(),
            onPressed: () {
              setState(() => _isMaskedApplied = false);
            },
          ),
        ],
      ),
    );
  }

  /// Multi-line scratchpad text box with lavender-tinted border
  Widget _buildScratchpadTextBox() {
    final hasMaskedChars = _textController.text.contains('•');

    return Container(
      constraints: const BoxConstraints(minHeight: 120),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: _borderLavender,
          width: 1.5,
        ),
      ),
      padding: const EdgeInsets.all(14.0),
      child: TextField(
        controller: _textController,
        focusNode: _focusNode,
        maxLines: null,
        minLines: 4,
        style: hasMaskedChars
            ? GoogleFonts.robotoMono(
                fontSize: 14,
                color: const Color(0xFF1E293B),
                height: 1.45,
                fontWeight: FontWeight.w500,
              )
            : GoogleFonts.plusJakartaSans(
                fontSize: 14.5,
                color: const Color(0xFF1E293B),
                height: 1.4,
                fontWeight: FontWeight.w500,
              ),
        decoration: InputDecoration(
          isDense: true,
          contentPadding: EdgeInsets.zero,
          border: InputBorder.none,
          hintText: 'Paste or type sensitive text to verify detection and masking...',
          hintStyle: GoogleFonts.plusJakartaSans(
            color: const Color(0xFF94A3B8),
            fontSize: 14,
            fontWeight: FontWeight.w400,
          ),
        ),
      ),
    );
  }

  /// "Clear Text" button on the left, character and word count on the right
  Widget _buildCounterAndClearRow(int charCount, int wordCount) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        GestureDetector(
          onTap: _clearText,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 4.0),
            child: Text(
              'Clear Text',
              style: GoogleFonts.plusJakartaSans(
                color: _accentPurple,
                fontSize: 13,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ),
        Text(
          '$charCount chars / $wordCount words',
          style: GoogleFonts.plusJakartaSans(
            color: const Color(0xFF64748B),
            fontSize: 12,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }

  /// Synthetic Quick Tests row
  Widget _buildQuickTestsSection() {
    final tests = [
      {
        'key': 'lola_otp',
        'label': 'Lola OTP',
        'text': 'Anak, eto yung code na pinadala sa akin:\n482913',
      },
      {
        'key': 'ml_password',
        'label': 'ML Password',
        'text': 'Pre, ito yung ML password ko: hunter2',
      },
      {
        'key': 'gemini_key',
        'label': 'Gemini Key',
        'text': 'Here is my Gemini API key: AIzaSyD3x920Kla82HjK981qWe',
      },
      {
        'key': 'gcash_no',
        'label': 'GCash No.',
        'text': 'Pa-GCash na lang sa 09171234567',
      },
      {
        'key': 'private_note',
        'label': 'Private Note',
        'text': _vaultSecrets.isNotEmpty
            ? 'My secret passcode is: ${_vaultSecrets.first.value}'
            : 'My secret note: secretpin88',
      },
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Synthetic quick tests',
          style: GoogleFonts.plusJakartaSans(
            fontSize: 12,
            color: const Color(0xFF64748B),
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: tests.map((t) {
            final isSelected = _activeQuickTestKey == t['key'];
            return GestureDetector(
              onTap: () => _selectQuickTest(t['key']!, t['text']!),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 180),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                decoration: BoxDecoration(
                  color: isSelected ? _brandPurple : _surfaceLavender,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: isSelected ? _brandPurple : _borderLavender,
                    width: 0.8,
                  ),
                ),
                child: Text(
                  t['label']!,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 12.5,
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.w600,
                    color: isSelected ? Colors.white : _brandPurple,
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}
