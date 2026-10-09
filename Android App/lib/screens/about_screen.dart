import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

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
                      // Curled Pangolin Mascot Avatar
                      Center(
                        child: Container(
                          width: 106,
                          height: 106,
                          decoration: const BoxDecoration(
                            color: _avatarLavender,
                            shape: BoxShape.circle,
                          ),
                          child: Center(
                            child: CustomPaint(
                              size: const Size(64, 64),
                              painter: CurledPangolinMascotPainter(),
                            ),
                          ),
                        ),
                      ),

                      const SizedBox(height: 18),

                      // Tanggolin Title
                      Text(
                        'Tanggolin (SecondLook\nKeyboard)',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 22,
                          fontWeight: FontWeight.w800,
                          color: const Color(0xFF1E293B),
                          height: 1.25,
                          letterSpacing: -0.4,
                        ),
                      ),

                      const SizedBox(height: 8),

                      // Subtitle / Tagline
                      Text(
                        'A second thought before you send • Depensa sa\nbawat pindot',
                        textAlign: TextAlign.center,
                        style: GoogleFonts.plusJakartaSans(
                          fontSize: 13.5,
                          color: const Color(0xFF64748B),
                          fontWeight: FontWeight.w500,
                          height: 1.35,
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

                      // Feature Card 5: Team & Attribution
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
}

/// Custom Vector Painter for Curled Palawan Pangolin Mascot
class CurledPangolinMascotPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    const brandColor = Color(0xFF581C87);
    final center = Offset(size.width / 2, size.height / 2);

    final shellPaint = Paint()
      ..color = brandColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6.0
      ..strokeCap = StrokeCap.round;

    // Curled body arc
    final rect = Rect.fromCircle(center: center, radius: size.width * 0.40);
    canvas.drawArc(rect, 0.45, 5.0, false, shellPaint);

    // Armored dorsal scales along back
    final scaleFill = Paint()
      ..color = brandColor
      ..style = PaintingStyle.fill;

    final scales = [
      Offset(center.dx - 2, center.dy - 16),
      Offset(center.dx + 10, center.dy - 14),
      Offset(center.dx + 16, center.dy - 4),
      Offset(center.dx + 16, center.dy + 8),
      Offset(center.dx + 8, center.dy + 16),
      Offset(center.dx - 4, center.dy + 18),
      Offset(center.dx - 14, center.dy + 12),
      Offset(center.dx - 18, center.dy - 2),
      Offset(center.dx - 12, center.dy - 12),
    ];

    for (final pt in scales) {
      final path = Path()
        ..moveTo(pt.dx, pt.dy - 4)
        ..lineTo(pt.dx + 4, pt.dy + 2)
        ..lineTo(pt.dx - 4, pt.dy + 2)
        ..close();
      canvas.drawPath(path, scaleFill);
    }

    // Curled tail tip
    final tailPaint = Paint()
      ..color = brandColor
      ..style = PaintingStyle.fill;
    canvas.drawCircle(Offset(center.dx - 12, center.dy + 2), 3.5, tailPaint);

    // Peaceful sleeping eye
    final eyePaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.8
      ..strokeCap = StrokeCap.round;
    canvas.drawArc(
      Rect.fromCircle(center: Offset(center.dx - 10, center.dy - 2), radius: 2.5),
      0.2,
      2.8,
      false,
      eyePaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
