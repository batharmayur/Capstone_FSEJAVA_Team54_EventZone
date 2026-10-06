const projects = [
  {
    id: 'p1',
    title: 'AI Career Guidance Platform',
    status: 'IN_REVIEW',
    progress: 72,
    owner: 'Aisha',
    dueDate: '2026-08-12'
  },
  {
    id: 'p2',
    title: 'Smart Attendance Monitoring',
    status: 'APPROVED',
    progress: 84,
    owner: 'Rohan',
    dueDate: '2026-08-20'
  },
  {
    id: 'p3',
    title: 'Campus Waste Analytics',
    status: 'DRAFT',
    progress: 38,
    owner: 'Meera',
    dueDate: '2026-08-30'
  }
];

export async function GET() {
  return Response.json({ projects, total: projects.length });
}
