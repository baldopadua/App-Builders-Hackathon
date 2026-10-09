import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/platform_bridge.dart';
import 'about_screen.dart';

class SecretsScreen extends StatefulWidget {
  const SecretsScreen({super.key});

  @override
  State<SecretsScreen> createState() => _SecretsScreenState();
}

class _SecretsScreenState extends State<SecretsScreen> {
  final TextEditingController _labelController = TextEditingController();
  final TextEditingController _valueController = TextEditingController();
  List<PrivateSecret> _secrets = [];
  bool _isLoading = false;
  bool _isSaving = false;

  static const Color _brandPurple = Color(0xFF581C87); // Deep rich violet
  static const Color _accentPurple = Color(0xFF7C3AED);
  static const Color _surfaceLavender = Color(0xFFFAF5FF);
  static const Color _borderLavender = Color(0xFFE9D5FF);

  @override
  void initState() {
    super.initState();
    _loadSecrets();
  }

  @override
  void dispose() {
    _labelController.dispose();
    _valueController.dispose();
    super.dispose();
  }

  Future<void> _loadSecrets() async {
    setState(() => _isLoading = true);
    final secrets = await PlatformBridge.getSecrets();
    if (mounted) {
      setState(() {
        _secrets = secrets;
        _isLoading = false;
      });
    }
  }

  Future<void> _onSaveSecret() async {
    if (_secrets.length >= 5) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Puno na ang vault (maximum 5 secrets).'),
          backgroundColor: _brandPurple,
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    final label = _labelController.text.trim();
    final value = _valueController.text.trim();

    if (label.isEmpty || value.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Pakilagay ang label at ang sikreto.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    setState(() => _isSaving = true);
    final created = await PlatformBridge.addSecret(label: label, value: value);
    if (mounted) {
      setState(() => _isSaving = false);
      if (created != null) {
        _labelController.clear();
        _valueController.clear();
        FocusScope.of(context).unfocus();
        _loadSecrets();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Protektado na ang "$label"!'),
            backgroundColor: _brandPurple,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  Future<void> _showDeleteConfirmation(PrivateSecret secret) async {
    final shouldDelete = await showDialog<bool>(
      context: context,
      builder: (context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          backgroundColor: Colors.white,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 28, 24, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Top Icon Pill
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: const Color(0xFFF3E8FF),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Icon(
                    Icons.delete_outline_rounded,
                    color: _accentPurple,
                    size: 28,
                  ),
                ),
                const SizedBox(height: 18),

                // Dialog Title
                Text(
                  'Delete ${secret.label}?',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 18.5,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF1F2937),
                    letterSpacing: -0.2,
                  ),
                ),
                const SizedBox(height: 10),

                // Dialog Body
                const Text(
                  'Hindi na ito poprotektahan ng Tanggolin. This removes the saved note from your device.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 13.5,
                    height: 1.4,
                    color: Color(0xFF4B5563),
                  ),
                ),
                const SizedBox(height: 24),

                // Actions: Cancel / Delete
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => Navigator.pop(context, false),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: const Color(0xFF4B5563),
                          side: const BorderSide(color: Color(0xFFE5E7EB)),
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: const Text(
                          'Cancel',
                          style: TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => Navigator.pop(context, true),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _brandPurple,
                          foregroundColor: Colors.white,
                          elevation: 0,
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: const Text(
                          'Delete',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );

    if (shouldDelete == true) {
      await PlatformBridge.deleteSecret(secret.id);
      _loadSecrets();
    }
  }

  @override
  Widget build(BuildContext context) {
    final isFull = _secrets.length >= 5;

    return Scaffold(
      backgroundColor: _brandPurple,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            // Top App Bar Header Area
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back_ios_new,
                        color: Colors.white, size: 20),
                    onPressed: () => Navigator.pop(context),
                  ),
                  const Expanded(
                    child: Text(
                      "Teach Me What's Private",
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
                      Icons.info_outline,
                      color: Colors.white,
                      size: 22,
                    ),
                    tooltip: 'About & Privacy',
                    onPressed: () {
                      Navigator.push(
                        context,
                        PageRouteBuilder(
                          pageBuilder:
                              (context, animation, secondaryAnimation) =>
                                  const AboutScreen(),
                          transitionsBuilder: (context, animation,
                              secondaryAnimation, child) {
                            const curve = Curves.easeInOutCubic;
                            final slide = Tween<Offset>(
                              begin: const Offset(1.0, 0.0),
                              end: Offset.zero,
                            ).animate(CurvedAnimation(
                                parent: animation, curve: curve));
                            final fade = Tween<double>(begin: 0.0, end: 1.0)
                                .animate(CurvedAnimation(
                                    parent: animation, curve: curve));
                            return SlideTransition(
                              position: slide,
                              child: FadeTransition(
                                opacity: fade,
                                child: child,
                              ),
                            );
                          },
                          transitionDuration:
                              const Duration(milliseconds: 280),
                          reverseTransitionDuration:
                              const Duration(milliseconds: 250),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),

            // White Main Body Container with Top Rounded Corners
            Expanded(
              child: Container(
                width: double.infinity,
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
                ),
                child: _isLoading
                    ? const Center(
                        child: CircularProgressIndicator(color: _brandPurple),
                      )
                    : SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(20, 22, 20, 36),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Capacity Pill Badge: "X / 5 Secrets Protected"
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 14, vertical: 6),
                              decoration: BoxDecoration(
                                color: const Color(0xFFF3E8FF),
                                borderRadius: BorderRadius.circular(16),
                              ),
                              child: Text(
                                '${_secrets.length} / 5 Secrets Protected',
                                style: const TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF6B21A8),
                                ),
                              ),
                            ),
                            const SizedBox(height: 16),

                            // Capacity Full Alert Banner (shown when 5 / 5)
                            if (isFull) ...[
                              Container(
                                padding: const EdgeInsets.all(14),
                                decoration: BoxDecoration(
                                  color: _surfaceLavender,
                                  borderRadius: BorderRadius.circular(14),
                                  border: Border.all(color: _borderLavender),
                                ),
                                child: const Row(
                                  children: [
                                    Icon(Icons.lock_outline_rounded,
                                        color: _brandPurple, size: 22),
                                    SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            'Puno na ang iyong proteksyon.',
                                            style: TextStyle(
                                              fontSize: 13.5,
                                              fontWeight: FontWeight.bold,
                                              color: _brandPurple,
                                            ),
                                          ),
                                          SizedBox(height: 2),
                                          Text(
                                            'Delete a protected note to make room for a new one.',
                                            style: TextStyle(
                                              fontSize: 12,
                                              color: Color(0xFF6B7280),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 16),
                            ],

                            // Add Private Note Form Card
                            _buildAddNoteCard(isFull),
                            const SizedBox(height: 28),

                            // Protected Notes Section Header
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                const Text(
                                  'Protected Notes',
                                  style: TextStyle(
                                    fontSize: 17,
                                    fontWeight: FontWeight.bold,
                                    color: Color(0xFF1F2937),
                                  ),
                                ),
                                Text(
                                  '${_secrets.length} saved',
                                  style: const TextStyle(
                                    fontSize: 13,
                                    color: Color(0xFF6B7280),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 14),

                            // Empty State OR List of Protected Notes
                            if (_secrets.isEmpty)
                              _buildEmptyState()
                            else
                              ListView.separated(
                                shrinkWrap: true,
                                physics: const NeverScrollableScrollPhysics(),
                                itemCount: _secrets.length,
                                separatorBuilder: (context, index) =>
                                    const SizedBox(height: 12),
                                itemBuilder: (context, index) {
                                  return _buildSecretItemCard(_secrets[index]);
                                },
                              ),
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

  /// Builds the "Add Private Note" form card
  Widget _buildAddNoteCard(bool isFull) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surfaceLavender,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: _borderLavender, width: 1.2),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Add Private Note',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: _brandPurple,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Save sensitive notes, project names, or family details. Tanggolin will intercept drafts that match their meaning or keywords.',
            style: TextStyle(
              fontSize: 12.5,
              height: 1.4,
              color: Color(0xFF4B5563),
            ),
          ),
          const SizedBox(height: 16),

          // Label Field
          TextField(
            controller: _labelController,
            enabled: !isFull && !_isSaving,
            decoration: InputDecoration(
              hintText: "Label (e.g., Jakim's Location, Project Sunflower)",
              hintStyle: const TextStyle(
                fontSize: 13,
                color: Color(0xFF9CA3AF),
              ),
              filled: true,
              fillColor: isFull ? const Color(0xFFF3F4F6) : Colors.white,
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: _accentPurple, width: 1.5),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Value Field
          TextField(
            controller: _valueController,
            enabled: !isFull && !_isSaving,
            decoration: InputDecoration(
              hintText: 'Enter the sensitive details to protect...',
              hintStyle: const TextStyle(
                fontSize: 13,
                color: Color(0xFF9CA3AF),
              ),
              filled: true,
              fillColor: isFull ? const Color(0xFFF3F4F6) : Colors.white,
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: Color(0xFFE5E7EB)),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: const BorderSide(color: _accentPurple, width: 1.5),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Submit Button: "I-save ang Sikreto / Protect Secret"
          SizedBox(
            width: double.infinity,
            height: 46,
            child: ElevatedButton(
              onPressed: (isFull || _isSaving) ? null : _onSaveSecret,
              style: ElevatedButton.styleFrom(
                backgroundColor: _brandPurple,
                disabledBackgroundColor: const Color(0xFFDDD6FE),
                foregroundColor: Colors.white,
                disabledForegroundColor: Colors.white,
                elevation: isFull ? 0 : 2,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: _isSaving
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'I-save ang Sikreto / Protect Secret',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }

  /// Builds the empty state container with pangolin icon
  Widget _buildEmptyState() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 36),
      child: Center(
        child: Column(
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: const BoxDecoration(
                color: Color(0xFFF3E8FF),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.shield_outlined,
                color: _accentPurple,
                size: 38,
              ),
            ),
            const SizedBox(height: 14),
            const Text(
              'No secrets saved yet.',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1F2937),
              ),
            ),
            const SizedBox(height: 4),
            const Text(
              'Add sensitive notes above to train your on-device shield.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                color: Color(0xFF6B7280),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Builds each protected secret card showing ONLY the label
  Widget _buildSecretItemCard(PrivateSecret secret) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: _surfaceLavender,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _borderLavender, width: 1.2),
      ),
      child: Row(
        children: [
          // Leading Shield Icon
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: const Color(0xFFEDE9FE),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(
              Icons.verified_user_outlined,
              color: _accentPurple,
              size: 20,
            ),
          ),
          const SizedBox(width: 14),

          // Label and Security Subtitle (never reveals the secret text)
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  secret.label,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: _brandPurple,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '••••••••  Encrypted AES-GCM',
                  style: GoogleFonts.robotoMono(
                    fontSize: 11.5,
                    color: const Color(0xFF6B7280),
                    letterSpacing: 0.6,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),

          // Delete Action Button
          IconButton(
            icon: const Icon(
              Icons.delete_outline_rounded,
              color: _accentPurple,
              size: 22,
            ),
            onPressed: () => _showDeleteConfirmation(secret),
          ),
        ],
      ),
    );
  }
}
