export type ProjectStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface DashboardProject {
  id: string;
  title: string;
  status: ProjectStatus;
  progress: number;
  description: string;
  dueDate?: string;
}
