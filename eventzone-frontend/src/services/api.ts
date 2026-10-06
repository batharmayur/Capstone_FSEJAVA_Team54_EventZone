import type { DashboardProject, ProjectStatus } from '../../types/project';

interface ProjectsResponse {
  total: number;
  projects: Array<{
    id: string;
    title: string;
    status: ProjectStatus;
    progress: number;
    owner?: string;
    dueDate?: string;
    description?: string;
  }>;
}

const fallbackProjects: DashboardProject[] = [
  {
    id: 'p1',
    title: 'Rock Night 2025',
    status: 'IN_REVIEW',
    progress: 72,
    description: 'Event production planning and sponsor approval workflow.',
    dueDate: '2026-08-12'
  },
  {
    id: 'p2',
    title: 'Championship Weekend',
    status: 'APPROVED',
    progress: 84,
    description: 'Venue coordination, ticketing, and volunteer staffing.',
    dueDate: '2026-08-20'
  },
  {
    id: 'p3',
    title: 'AI Product Meetup',
    status: 'DRAFT',
    progress: 38,
    description: 'Speaker shortlist, agenda review, and launch planning.',
    dueDate: '2026-08-30'
  }
];

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

function withBase(path: string) {
  return apiBaseUrl ? `${apiBaseUrl}${path}` : path;
}

export async function fetchProjects(): Promise<DashboardProject[]> {
  try {
    const response = await fetch(withBase('/api/projects'));

    if (!response.ok) {
      throw new Error(`Failed to load projects: ${response.status}`);
    }

    const data = (await response.json()) as ProjectsResponse;

    return data.projects.map((project) => ({
      id: project.id,
      title: project.title,
      status: project.status,
      progress: project.progress,
      description: project.description ?? 'Project delivery status tracked from the event operations workspace.',
      dueDate: project.dueDate
    }));
  } catch {
    return fallbackProjects;
  }
}