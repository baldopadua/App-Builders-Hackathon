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

  String? _activeQuickTestKey;

  // Shared Brand Palette aligned with SecretsScreen
  static const Color _brandPurple = Color(0xFF581C87); // Deep rich violet
  static const Color _accentPurple = Color(0xFF7C3AED);
  static const Color _surfaceLavender = Color(0xFFFAF5FF);
  static const Color _borderLavender = Color(0xFFE9D5FF);

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
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 150), () {
      final text = _textController.text;
      if (text.isEmpty && mounted) {
        setState(() {
          _activeQuickTestKey = null;
        });
      }
    });
  }

  void _clearText() {
    setState(() {
      _textController.clear();
      _activeQuickTestKey = null;
    });
  }

  void _selectQuickTest(String key, String text) {
    setState(() {
      _activeQuickTestKey = key;
      _textController.text = text;
    });
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

  /// Seamless in-place pure crossfade page route transition (no swipe / slide motion)
  Route _createSeamlessRoute(Widget page) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return FadeTransition(
          opacity: CurvedAnimation(
            parent: animation,
            curve: Curves.easeInOut,
          ),
          child: child,
        );
      },
      transitionDuration: const Duration(milliseconds: 220),
      reverseTransitionDuration: const Duration(milliseconds: 200),
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
        'key': 'otp_code',
        'label': 'OTP Code',
        'text': 'Here is the verification code sent to me:\n482913',
      },
      {
        'key': 'ml_password',
        'label': 'Account Password',
        'text': 'Hey, here is my account password: hunter2',
      },
      {
        'key': 'gemini_key',
        'label': 'Gemini Key',
        'text': 'Here is my Gemini API key: AIzaSyD3x920Kla82HjK981qWe',
      },
      {
        'key': 'phone_no',
        'label': 'Phone Number',
        'text': 'Send payment to my mobile: 09171234567',
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
