import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  const passwordHash = await bcrypt.hash('password123', 10);

  const admin = await prisma.user.upsert({
    where: { email: 'admin@capstoneflow.dev' },
    update: {},
    create: {
      email: 'admin@capstoneflow.dev',
      passwordHash,
      name: 'Admin User',
      role: 'ADMIN'
    }
  });

  await prisma.user.upsert({
    where: { email: 'student@capstoneflow.dev' },
    update: {},
    create: {
      email: 'student@capstoneflow.dev',
      passwordHash,
      name: 'Student User',
      role: 'STUDENT'
    }
  });

  await prisma.project.createMany({
    data: [
      {
        title: 'AI Career Guidance Platform',
        description: 'Build a recommendation system for student learning pathways and career options.',
        status: 'IN_REVIEW',
        progress: 72,
        studentId: admin.id
      },
      {
        title: 'Smart Attendance Monitoring',
        description: 'Develop an automated classroom attendance tracking solution using computer vision.',
        status: 'APPROVED',
        progress: 84,
        studentId: admin.id
      },
      {
        title: 'Campus Waste Analytics',
        description: 'Analyze waste generation and optimize recycling schedules for campus management.',
        status: 'DRAFT',
        progress: 38,
        studentId: admin.id
      }
    ],
    skipDuplicates: true
  });

  console.log('Seed data applied successfully.');
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
