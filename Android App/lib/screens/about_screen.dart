import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../services/platform_bridge.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  static const Color _brandPurple = Color(0xFF581C87); // Deep rich violet
  static const Color _accentPurple = Color(0xFF7C3AED);
  static const Color _surfaceLavender = Color(0xFFFAF5FF);
  static const Color _borderLavender = Color(0xFFE9D5FF);
  static const Color _avatarLavender = Color(0xFFF3E8FF);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _brandPurple,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            // Top App Bar Header Area aligned with Secrets & Practice Screens
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(
                      Icons.arrow_back_ios_new,
                      color: Colors.white,
                      size: 20,
                    ),
                    onPressed: () => Navigator.pop(context),
                  ),
                  const Expanded(
                    child: Text(
                      'About & Privacy',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        letterSpacing: -0.3,
                      ),
                    ),
                  ),
                  const SizedBox(width: 48), // Balance leading back button
                ],
              ),
            ),

            // White Main Body Container with Top Rounded Corners (radius: 30)
            Expanded(
              child: Container(
                width: double.infinity,
                decoration: const BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
                ),
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(20, 24, 20, 40),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Tanggolin Mascot Avatar using official SVG
                      Center(
                        child: Container(
                          width: 106,
                          height: 106,
                          decoration: const BoxDecoration(
                            color: _avatarLavender,
                            shape: BoxShape.circle,
                          ),
                          padding: const EdgeInsets.all(18),
                          child: Image.asset(
                            'assets/icons/tanggolin_icon.png',
                            fit: BoxFit.contain,
                          ),
                        ),
                      ),

                      const SizedBox(height: 18),

                      // Tanggolin Title
                      Text(
                        'Tanggolin',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 24,
                          fontWeight: FontWeight.w800,
                          color: const Color(0xFF1E293B),
                          letterSpacing: -0.4,
                        ),
                      ),

                      const SizedBox(height: 6),

                      // Subtitle / Tagline
                      Text(
                        'Local AI Privacy Keyboard',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 14,
                          color: const Color(0xFF64748B),
                          fontWeight: FontWeight.w600,
                          letterSpacing: 0.1,
                        ),
                      ),

                      const SizedBox(height: 22),

                      // Palawan Pangolin Lore / Quote Card
                      Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: _surfaceLavender,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: _borderLavender, width: 1.0),
                        ),
                        child: Text(
                          '“When the Palawan pangolin senses danger, it curls into an impenetrable armored ball. Tanggolin does the same for your personal data—stepping in to seal sensitive information before it leaves your phone.”',
                          style: GoogleFonts.plusJakartaSans(
                            fontSize: 13.5,
                            color: const Color(0xFF475569),
                            height: 1.45,
                            fontWeight: FontWeight.w400,
                          ),
                        ),
                      ),

                      const SizedBox(height: 24),

                      // Privacy Architecture Breakdown Section Header
                      Text(
                        'Privacy Architecture Breakdown',
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 17,
                          fontWeight: FontWeight.w800,
                          color: const Color(0xFF1E293B),
                          letterSpacing: -0.3,
                        ),
                      ),

                      const SizedBox(height: 14),

                      // Feature Card 1: No Internet Permission
                      _buildFeatureCard(
                        title: 'No Internet Permission',
                        description:
                            'Declared with zero network permissions in AndroidManifest.xml. Impossible to leak data to the cloud.',
                      ),

                      const SizedBox(height: 12),

                      // Feature Card 2: Dual On-Device AI Models
                      _buildFeatureCard(
                        title: 'Dual On-Device AI Models',
                        description:
                            'bert-small-pii (27 MB) and all-MiniLM-L6-v2 (22 MB) run locally via ONNX Runtime Mobile.',
                      ),

                      const SizedBox(height: 12),

                      // Feature Card 3: Hardware-Backed Encryption
                      _buildFeatureCard(
                        title: 'Hardware-Backed Encryption',
                        description:
                            'Saved private notes are encrypted using AES-GCM via the Android Keystore.',
                      ),

                      const SizedBox(height: 12),

                      // Feature Card 4: Zero Keystroke Storage
                      _buildFeatureCard(
                        title: 'Zero Keystroke Storage',
                        description:
                            'Keystrokes exist strictly in temporary memory during typing checks and are never logged or persisted.',
                      ),

                      const SizedBox(height: 12),

                      const SizedBox(height: 12),

                      // Feature Card 5: Terms & Conditions
                      InkWell(
                        onTap: () => showTermsAndConditionsSheet(context),
                        borderRadius: BorderRadius.circular(14),
                        child: Container(
                          padding: const EdgeInsets.all(16.0),
                          decoration: BoxDecoration(
                            color: _surfaceLavender,
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: _borderLavender, width: 1.0),
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 40,
                                height: 40,
                                decoration: BoxDecoration(
                                  color: _avatarLavender,
                                  borderRadius: BorderRadius.circular(10),
                                ),
                                child: const Icon(
                                  Icons.description_outlined,
                                  color: _accentPurple,
                                  size: 22,
                                ),
                              ),
                              const SizedBox(width: 14),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: const [
                                    Text(
                                      'Terms & Conditions',
                                      style: TextStyle(
                                        fontSize: 15,
                                        fontWeight: FontWeight.bold,
                                        color: Color(0xFF1E293B),
                                      ),
                                    ),
                                    SizedBox(height: 3),
                                    Text(
                                      'How user data is handled & protected',
                                      style: TextStyle(
                                        fontSize: 12.5,
                                        color: Color(0xFF64748B),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const Icon(
                                Icons.arrow_forward_ios_rounded,
                                size: 16,
                                color: _accentPurple,
                              ),
                            ],
                          ),
                        ),
                      ),

                      const SizedBox(height: 12),

                      // Feature Card 6: Team & Attribution
                      Container(
                        padding: const EdgeInsets.all(16.0),
                        decoration: BoxDecoration(
                          color: _surfaceLavender,
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: _borderLavender, width: 1.0),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: const [
                            Text(
                              'Team & Attribution',
                              style: TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF1E293B),
                              ),
                            ),
                            SizedBox(height: 6),
                            Text(
                              'AppBuildersPH Hackathon 2026 entry.',
                              style: TextStyle(
                                fontSize: 13,
                                color: Color(0xFF64748B),
                                height: 1.35,
                              ),
                            ),
                            SizedBox(height: 8),
                            Text(
                              'Team members: Klarenz, Annie, Charles, Jakim',
                              style: TextStyle(
                                fontSize: 13,
                                color: Color(0xFF64748B),
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ],
                        ),
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

  Widget _buildFeatureCard({
    required String title,
    required String description,
  }) {
    return Container(
      padding: const EdgeInsets.all(16.0),
      decoration: BoxDecoration(
        color: _surfaceLavender,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: _borderLavender, width: 1.0),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: _accentPurple,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            description,
            style: const TextStyle(
              fontSize: 13,
              color: Color(0xFF64748B),
              height: 1.35,
            ),
          ),
        ],
      ),
    );
  }

  /// Displays comprehensive Terms & Conditions and Data Handling disclosure modal sheet
  static void showTermsAndConditionsSheet(BuildContext context) async {
    final alreadyAccepted = await PlatformBridge.hasAcceptedTerms();
    if (!context.mounted) return;

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
            borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
          ),
          child: Column(
            children: [
              // Drag handle
              Container(
                margin: const EdgeInsets.only(top: 12, bottom: 8),
                width: 44,
                height: 4.5,
                decoration: BoxDecoration(
                  color: const Color(0xFFE2E8F0),
                  borderRadius: BorderRadius.circular(3),
                ),
              ),

              // Title Header
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: _avatarLavender,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Icon(
                        Icons.shield_outlined,
                        color: _accentPurple,
                        size: 24,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Terms & Conditions',
                            style: GoogleFonts.plusJakartaSans(
                              fontSize: 19,
                              fontWeight: FontWeight.w800,
                              color: const Color(0xFF1E293B),
                            ),
                          ),
                          const SizedBox(height: 2),
                          const Text(
                            'Data handling & privacy disclosure',
                            style: TextStyle(
                              fontSize: 12.5,
                              color: Color(0xFF64748B),
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, color: Color(0xFF64748B)),
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
              ),
              const Divider(height: 1, thickness: 1, color: Color(0xFFF1F5F9)),

              // Scrollable Terms Content
              Expanded(
                child: ListView(
                  controller: scrollController,
                  padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
                  children: [
                    _buildTermsSection(
                      number: '1',
                      title: '100% On-Device Processing Guarantee',
                      body:
                          'All text processing, heuristic scanning, and AI inference execute exclusively on your physical phone hardware. At no point is any text, pattern, or keystroke sent to cloud servers or third-party APIs.',
                    ),
                    _buildTermsSection(
                      number: '2',
                      title: 'Zero Network Connectivity',
                      body:
                          'Tanggolin is deliberately engineered with zero network permissions. The android.permission.INTERNET flag is completely omitted from the application manifest, making cloud transmission technically and physically impossible.',
                    ),
                    _buildTermsSection(
                      number: '3',
                      title: 'Zero Keystroke Storage & Logging',
                      body:
                          'Tanggolin does not log, record, or track your keystrokes. Input connection buffers reside strictly in volatile RAM memory during real-time privacy scanning and are discarded immediately. No typing history is ever saved to disk.',
                    ),
                    _buildTermsSection(
                      number: '4',
                      title: 'Hardware-Backed Encryption Vault',
                      body:
                          'Custom private notes and sensitive phrases you save in your Private Vault are encrypted using AES-256-GCM. Encryption keys are generated and protected by the hardware-backed Android Keystore system.',
                    ),
                    _buildTermsSection(
                      number: '5',
                      title: 'User Ownership & Full Control',
                      body:
                          'You maintain total ownership of your data. You may add, edit, or permanently delete encrypted vault items at any time. Uninstalling Tanggolin completely removes all local encrypted storage.',
                    ),
                    _buildTermsSection(
                      number: '6',
                      title: 'Assistive Safety Shield Disclaimer',
                      body:
                          'Tanggolin is designed as an on-device safety net to help prevent accidental leaks of sensitive credentials, OTPs, and private data. While our rule engines and models are tuned for high precision, users remain responsible for the content they choose to send or share.',
                    ),
                    _buildTermsSection(
                      number: '7',
                      title: 'No Advertising or Trackers',
                      body:
                          'Tanggolin contains zero advertising SDKs, zero behavioral trackers, and zero analytical trackers. Your phone remains clean, private, and fast.',
                    ),
                    const SizedBox(height: 16),

                    // Acceptance status button/badge
                    if (alreadyAccepted)
                      Container(
                        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 16),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF1F5F9),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: const Color(0xFFCBD5E1)),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(
                              Icons.check_circle_rounded,
                              color: Color(0xFF10B981),
                              size: 20,
                            ),
                            const SizedBox(width: 8),
                            Text(
                              'Already accepted during onboarding',
                              style: GoogleFonts.plusJakartaSans(
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                                color: const Color(0xFF475569),
                              ),
                            ),
                          ],
                        ),
                      )
                    else
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: _brandPurple,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                          elevation: 0,
                        ),
                        onPressed: () async {
                          await PlatformBridge.setAcceptedTerms(true);
                          if (ctx.mounted) Navigator.pop(ctx);
                        },
                        child: const Text(
                          'I Understand & Accept',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.bold,
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
      padding: const EdgeInsets.only(bottom: 18),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 26,
            height: 26,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
              color: _surfaceLavender,
              shape: BoxShape.circle,
            ),
            child: Text(
              number,
              style: const TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.bold,
                color: _accentPurple,
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
                  style: const TextStyle(
                    fontSize: 14.5,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF1E293B),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  body,
                  style: const TextStyle(
                    fontSize: 13,
                    color: Color(0xFF475569),
                    height: 1.4,
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


