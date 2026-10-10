import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../main.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTokens.surfaceWhite,
      appBar: AppBar(
        titleSpacing: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18),
          color: AppTokens.textPrimary,
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'About & privacy',
          style: GoogleFonts.plusJakartaSans(
            fontSize: 19,
            fontWeight: FontWeight.w700,
            color: AppTokens.textPrimary,
            letterSpacing: -0.3,
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Pangolin Mark, Tanggolin, and Privacy keyboard
              Center(
                child: Column(
                  children: [
                    Container(
                      width: 80,
                      height: 80,
                      decoration: const BoxDecoration(
                        color: AppTokens.lavenderSelected,
                        shape: BoxShape.circle,
                      ),
                      padding: const EdgeInsets.all(14),
                      child: Image.asset(
                        'assets/icons/tanggolin_icon.png',
                        fit: BoxFit.contain,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Tanggolin',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 22,
                        fontWeight: FontWeight.w800,
                        color: AppTokens.textPrimary,
                        letterSpacing: -0.3,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Privacy keyboard',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: AppTokens.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 20),

              // 2. Short brand statement (replacing long quote)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                decoration: BoxDecoration(
                  color: AppTokens.surfaceTinted,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTokens.borderLavender),
                ),
                child: Text(
                  'A pangolin curls up to protect itself. Tanggolin helps you protect private details in your drafts.',
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 14,
                    height: 1.5,
                    fontWeight: FontWeight.w400,
                    color: AppTokens.brandPurple,
                  ),
                ),
              ),

              const SizedBox(height: 28),

              // 3. How your privacy works (Plain fact rows separated by dividers)
              Text(
                'How your privacy works',
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                  color: AppTokens.textPrimary,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 12),

              _buildFactRow(
                icon: Icons.smartphone_rounded,
                title: 'Checks run on this phone',
                description:
                    'Draft checks run locally on your device. No text is sent to cloud servers.',
              ),
              const Divider(color: Color(0xFFF1F5F9), height: 20),

              _buildFactRow(
                icon: Icons.lock_outline_rounded,
                title: 'Notes are encrypted',
                description:
                    'Saved private notes are encrypted using AES-GCM and stored only on this device.',
              ),
              const Divider(color: Color(0xFFF1F5F9), height: 20),

              _buildFactRow(
                icon: Icons.history_toggle_off_rounded,
                title: 'Typed text is not saved',
                description:
                    'Keystrokes exist strictly in temporary memory during checks and are never logged or stored.',
              ),
              const Divider(color: Color(0xFFF1F5F9), height: 20),

              const SizedBox(height: 12),

              // 4. Technical details (Collapsed by default)
              Theme(
                data: Theme.of(context).copyWith(
                  dividerColor: Colors.transparent,
                ),
                child: ExpansionTile(
                  tilePadding: EdgeInsets.zero,
                  childrenPadding: const EdgeInsets.only(bottom: 12),
                  title: Text(
                    'Technical details',
                    style: GoogleFonts.plusJakartaSans(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: AppTokens.textPrimary,
                    ),
                  ),
                  iconColor: AppTokens.brandAccent,
                  collapsedIconColor: AppTokens.textSecondary,
                  children: [
                    _buildTechItem(
                      'Dual on-device AI models',
                      'Runs bert-small-pii (27 MB) for entity detection and all-MiniLM-L6-v2 (22 MB) for semantic matching locally via ONNX Runtime Mobile.',
                    ),
                    const SizedBox(height: 10),
                    _buildTechItem(
                      'Encryption & key management',
                      'AES-GCM encryption with keys generated and secured by the Android Keystore.',
                    ),
                    const SizedBox(height: 10),
                    _buildTechItem(
                      'Zero network permissions',
                      'android.permission.INTERNET is completely omitted from AndroidManifest.xml.',
                    ),
                  ],
                ),
              ),

              const Divider(color: Color(0xFFF1F5F9), height: 20),

              // 5. Terms & conditions row
              InkWell(
                onTap: () => showTermsAndConditionsSheet(context),
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.description_outlined,
                        size: 20,
                        color: AppTokens.brandAccent,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Terms & conditions',
                          style: GoogleFonts.plusJakartaSans(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: AppTokens.textPrimary,
                          ),
                        ),
                      ),
                      const Icon(
                        Icons.arrow_forward_ios_rounded,
                        size: 14,
                        color: Color(0xFF94A3B8),
                      ),
                    ],
                  ),
                ),
              ),

              const Divider(color: Color(0xFFF1F5F9), height: 20),

              const SizedBox(height: 20),

              // 6. Quieter footer content
              Center(
                child: Column(
                  children: [
                    Text(
                      'AppBuildersPH Hackathon 2026 entry',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: AppTokens.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Version 1.0.0',
                      style: GoogleFonts.plusJakartaSans(
                        fontSize: 12,
                        color: const Color(0xFF94A3B8),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  /// Fact row for privacy breakdown
  Widget _buildFactRow({
    required IconData icon,
    required String title,
    required String description,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 20, color: AppTokens.brandAccent),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: AppTokens.textPrimary,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                description,
                style: GoogleFonts.plusJakartaSans(
                  fontSize: 13.5,
                  height: 1.45,
                  color: AppTokens.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  /// Technical explanation item
  Widget _buildTechItem(String title, String description) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppTokens.surfaceTinted,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: GoogleFonts.plusJakartaSans(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: AppTokens.brandPurple,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            description,
            style: GoogleFonts.plusJakartaSans(
              fontSize: 12.5,
              height: 1.4,
              color: AppTokens.textSecondary,
            ),
          ),
        ],
      ),
    );
  }

  /// Clean, bounded Terms & Conditions modal sheet (Section 15)
  static void showTermsAndConditionsSheet(BuildContext context,
      {bool isFirstTimeAcceptance = false}) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.85,
        maxChildSize: 0.95,
        minChildSize: 0.5,
        builder: (_, scrollController) => Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: Column(
            children: [
              // Pinned header with clean drag handle & close control
              Container(
                margin: const EdgeInsets.only(top: 10, bottom: 6),
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: const Color(0xFFCBD5E1),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 6, 12, 12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Terms & conditions',
                          style: GoogleFonts.plusJakartaSans(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: AppTokens.textPrimary,
                          ),
                        ),
                        Text(
                          'Data handling and privacy disclosures',
                          style: GoogleFonts.plusJakartaSans(
                            fontSize: 12.5,
                            color: AppTokens.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 22),
                      color: AppTokens.textSecondary,
                      tooltip: 'Close terms',
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
              ),
              const Divider(color: Color(0xFFF1F5F9), height: 1),

              // Bounded Scrollable Body
              Expanded(
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
                  children: [
                    _buildTermsSection(
                      number: '1',
                      title: 'On-device processing',
                      body:
                          'All text processing and privacy scanning execute strictly on this device. Draft text is not transmitted to cloud servers.',
                    ),
                    _buildTermsSection(
                      number: '2',
                      title: 'Zero network connectivity',
                      body:
                          'Tanggolin is engineered without internet permissions (android.permission.INTERNET is omitted), ensuring no network traffic can originate from the keyboard.',
                    ),
                    _buildTermsSection(
                      number: '3',
                      title: 'No keystroke logging',
                      body:
                          'Tanggolin does not log or persist typing history. Text buffers exist only in temporary memory during active checking.',
                    ),
                    _buildTermsSection(
                      number: '4',
                      title: 'Encrypted private notes',
                      body:
                          'Saved private notes are encrypted with AES-GCM using keys stored in the Android Keystore.',
                    ),
                    _buildTermsSection(
                      number: '5',
                      title: 'User ownership and control',
                      body:
                          'You maintain complete control of your saved notes. You may delete them at any time from Private notes.',
                    ),
                    _buildTermsSection(
                      number: '6',
                      title: 'Assistive shield disclaimer',
                      body:
                          'Tanggolin assists you by flagging sensitive details before sharing. You remain responsible for the content you choose to send.',
                    ),
                    _buildTermsSection(
                      number: '7',
                      title: 'No analytics or trackers',
                      body:
                          'Tanggolin contains zero advertising SDKs, analytics tracking, or behavioral profiling.',
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: ElevatedButton(
                        onPressed: () => Navigator.pop(ctx),
                        child: Text(
                          isFirstTimeAcceptance
                              ? 'I Understand & Accept'
                              : 'Done',
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

  static Widget _buildTermsSection({
    required String number,
    required String title,
    required String body,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 22,
            height: 22,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
              color: AppTokens.surfaceTinted,
              shape: BoxShape.circle,
            ),
            child: Text(
              number,
              style: GoogleFonts.plusJakartaSans(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: AppTokens.brandPurple,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 14.5,
                    fontWeight: FontWeight.w600,
                    color: AppTokens.textPrimary,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  body,
                  style: GoogleFonts.plusJakartaSans(
                    fontSize: 13,
                    height: 1.45,
                    color: AppTokens.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
