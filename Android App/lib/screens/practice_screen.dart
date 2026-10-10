import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../main.dart';
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
  String? _selectedSampleKey;

  final List<Map<String, String>> _samples = [
    {
      'key': 'password',
      'label': 'Password',
      'text': 'Hey, here is my account password: hunter2',
    },
    {
      'key': 'otp',
      'label': 'OTP',
      'text': 'Here is the verification code sent to me:\n482913',
    },
    {
      'key': 'phone',
      'label': 'Phone number',
      'text': 'Send payment to my mobile: 09171234567',
    },
    {
      'key': 'api_key',
      'label': 'API key',
      'text': 'Here is my API key: AIzaSyD3x920Kla82HjK981qWe',
    },
    {
      'key': 'private_note',
      'label': 'Private note',
      'text': 'Meeting notes for Project Orion: safe discussion',
    },
  ];

  @override
  void initState() {
    super.initState();
    _textController.addListener(_onTextUpdated);
  }

  @override
  void dispose() {
    _textController.removeListener(_onTextUpdated);
    _textController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _onTextUpdated() {
    if (mounted) {
      setState(() {
        if (_textController.text.isEmpty) {
          _selectedSampleKey = null;
        } else if (_selectedSampleKey != null) {
          final selected = _samples.firstWhere(
            (s) => s['key'] == _selectedSampleKey,
            orElse: () => {},
          );
          if (selected.isNotEmpty && _textController.text != selected['text']) {
            _selectedSampleKey = null;
          }
        }
      });
    }
  }

  void _onSelectSample(String key, String text) {
    setState(() {
      _selectedSampleKey = key;
      _textController.text = text;
      _textController.selection = TextSelection.fromPosition(
        TextPosition(offset: text.length),
      );
    });
  }

  void _clearText() {
    if (_textController.text.isEmpty) return;
    setState(() {
      _textController.clear();
      _selectedSampleKey = null;
    });
  }

  void _navigateToSecrets() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const SecretsScreen()),
    );
  }

  void _navigateToAbout() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AboutScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = _textController.text;
    final charCount = text.length;
    final hasText = text.isNotEmpty;

    return Scaffold(
      backgroundColor: AppTokens.surfaceWhite,
      appBar: AppBar(
        titleSpacing: 20,
        title: Row(
          children: [
            Image.asset(
              'assets/icons/tanggolin_icon.png',
              width: 26,
              height: 26,
              fit: BoxFit.contain,
            ),
            const SizedBox(width: 10),
            Text(
              'Try protection',
              style: GoogleFonts.plusJakartaSans(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: AppTokens.textPrimary,
                letterSpacing: -0.3,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(
              Icons.bookmark_outline_rounded,
              color: AppTokens.textPrimary,
              size: 22,
            ),
            tooltip: 'Private notes',
            onPressed: _navigateToSecrets,
          ),
          IconButton(
            icon: const Icon(
              Icons.info_outline_rounded,
              color: AppTokens.textPrimary,
              size: 22,
            ),
            tooltip: 'About & privacy',
            onPressed: _navigateToAbout,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Safe Examples Header
              Text(
                'Try a safe example',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: AppTokens.textPrimary,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Samples are fictional. Nothing is sent here.',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w400,
                  color: AppTokens.textSecondary,
                ),
              ),
              const SizedBox(height: 14),

              // Stable, 2-Column Options Grid (Equal heights, no width shifting, isolated taps)
              LayoutBuilder(
                builder: (context, constraints) {
                  const spacing = 10.0;
                  final itemWidth = (constraints.maxWidth - spacing) / 2;

                  return Wrap(
                    spacing: spacing,
                    runSpacing: spacing,
                    children: _samples.map((sample) {
                      final isSelected = _selectedSampleKey == sample['key'];
                      return SizedBox(
                        width: itemWidth,
                        child: InkWell(
                          onTap: () =>
                              _onSelectSample(sample['key']!, sample['text']!),
                          borderRadius: BorderRadius.circular(12),
                          child: AnimatedContainer(
                            duration: const Duration(milliseconds: 150),
                            height: 48,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 14,
                            ),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? AppTokens.surfaceTinted
                                  : AppTokens.surfaceWhite,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: isSelected
                                    ? AppTokens.brandPurple
                                    : AppTokens.borderNeutral,
                                width: isSelected ? 1.8 : 1.0,
                              ),
                            ),
                            child: Row(
                              children: [
                                // Radio-style check indicator with fixed bounds (no layout jump)
                                Container(
                                  width: 18,
                                  height: 18,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    color: isSelected
                                        ? AppTokens.brandPurple
                                        : Colors.transparent,
                                    border: Border.all(
                                      color: isSelected
                                          ? AppTokens.brandPurple
                                          : const Color(0xFFCBD5E1),
                                      width: 1.5,
                                    ),
                                  ),
                                  child: isSelected
                                      ? const Icon(
                                          Icons.check,
                                          size: 12,
                                          color: Colors.white,
                                        )
                                      : null,
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Text(
                                    sample['label']!,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: GoogleFonts.plusJakartaSans(
                                      fontSize: 13.5,
                                      fontWeight: isSelected
                                          ? FontWeight.w700
                                          : FontWeight.w500,
                                      color: isSelected
                                          ? AppTokens.brandPurple
                                          : AppTokens.textPrimary,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  );
                },
              ),

              const SizedBox(height: 24),

              // Test message Header
              Text(
                'Test message',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: AppTokens.textPrimary,
                ),
              ),
              const SizedBox(height: 8),

              // Test message Input Box
              Container(
                constraints: const BoxConstraints(minHeight: 144),
                decoration: BoxDecoration(
                  color: AppTokens.surfaceWhite,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: _focusNode.hasFocus
                        ? AppTokens.brandAccent
                        : AppTokens.borderNeutral,
                    width: _focusNode.hasFocus ? 1.5 : 1.0,
                  ),
                ),
                padding: const EdgeInsets.all(14),
                child: TextField(
                  controller: _textController,
                  focusNode: _focusNode,
                  maxLines: null,
                  minLines: 4,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 15,
                    color: AppTokens.textPrimary,
                    height: 1.45,
                    fontWeight: FontWeight.w400,
                  ),
                  decoration: InputDecoration(
                    isDense: true,
                    contentPadding: EdgeInsets.zero,
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    hintText: 'Choose an example or type a test message.',
                    hintStyle: GoogleFonts.plusJakartaSans(
                      color: AppTokens.textSecondary,
                      fontSize: 14.5,
                      fontWeight: FontWeight.w400,
                    ),
                  ),
                ),
              ),

              const SizedBox(height: 8),

              // Clear button & Character Count Row
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  InkWell(
                    onTap: hasText ? _clearText : null,
                    borderRadius: BorderRadius.circular(6),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 4,
                        vertical: 10,
                      ),
                      child: Text(
                        'Clear',
                        style: GoogleFonts.plusJakartaSans(
                          color: hasText
                              ? AppTokens.brandAccent
                              : const Color(0xFF94A3B8),
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                  Text(
                    '$charCount ${charCount == 1 ? "character" : "characters"}',
                    style: GoogleFonts.plusJakartaSans(
                      color: AppTokens.textSecondary,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 20),

              // Helper instruction banner
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                decoration: BoxDecoration(
                  color: AppTokens.surfaceTinted,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTokens.borderLavender),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.keyboard_outlined,
                      size: 20,
                      color: AppTokens.brandPurple,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Use the Tanggolin keyboard to see a warning.',
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w500,
                          color: AppTokens.brandPurple,
                        ),
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
