import { Search, Bell, ChevronDown, FolderKanban, Users, Clock3 } from 'lucide-react';
import Link from 'next/link';

const metrics = [
  { label: 'Active Projects', value: '128', change: '+14%', icon: FolderKanban },
  { label: 'Team Members', value: '48', change: '+8%', icon: Users },
  { label: 'Pending Reviews', value: '19', change: '-6%', icon: Clock3 }
];

const projects = [
  { title: 'AI Career Guidance Platform', status: 'In Review', progress: 72, owner: 'Aisha', due: '12 Aug' },
  { title: 'Smart Attendance Monitoring', status: 'Approved', progress: 84, owner: 'Rohan', due: '20 Aug' },
  { title: 'Campus Waste Analytics', status: 'Draft', progress: 38, owner: 'Meera', due: '30 Aug' }
];

export default function DashboardPage() {
  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div>
            <p className="text-sm text-slate-500">Welcome back</p>
            <h1 className="text-2xl font-bold text-slate-900">Capstone dashboard</h1>
          </div>
          <div className="flex items-center gap-3">
            <button className="rounded-xl border border-slate-200 bg-white p-2 text-slate-600">
              <Search size={18} />
            </button>
            <button className="rounded-xl border border-slate-200 bg-white p-2 text-slate-600">
              <Bell size={18} />
            </button>
            <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700">
              Faculty Panel <ChevronDown size={16} />
            </button>
          </div>
        </header>

        <section className="mb-8 grid gap-4 md:grid-cols-3">
          {metrics.map(({ label, value, change, icon: Icon }) => (
            <div key={label} className="card-surface p-5">
              <div className="mb-6 flex items-center justify-between">
                <p className="text-sm text-slate-500">{label}</p>
                <div className="rounded-lg bg-brand-50 p-2 text-brand-700">
                  <Icon size={18} />
                </div>
              </div>
              <div className="flex items-end justify-between">
                <span className="text-3xl font-bold text-slate-900">{value}</span>
                <span className="text-sm font-semibold text-emerald-600">{change}</span>
              </div>
            </div>
          ))}
        </section>

        <section className="card-surface overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Project pipeline</h2>
            </div>
            <Link href="/" className="text-sm font-medium text-brand-700">View all</Link>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left">
              <thead className="bg-slate-50 text-sm text-slate-500">
                <tr>
                  <th className="px-6 py-3 font-medium">Project</th>
                  <th className="px-6 py-3 font-medium">Status</th>
                  <th className="px-6 py-3 font-medium">Owner</th>
                  <th className="px-6 py-3 font-medium">Progress</th>
                  <th className="px-6 py-3 font-medium">Due date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white text-sm text-slate-700">
                {projects.map((project) => (
                  <tr key={project.title}>
                    <td className="px-6 py-4 font-medium text-slate-900">{project.title}</td>
                    <td className="px-6 py-4">
                      <span className="inline-flex rounded-full bg-brand-50 px-2.5 py-1 text-xs font-medium text-brand-700">
                        {project.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">{project.owner}</td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-2 w-28 overflow-hidden rounded-full bg-slate-200">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-brand-500 to-emerald-500"
                            style={{ width: `${project.progress}%` }}
                          />
                        </div>
                        <span>{project.progress}%</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">{project.due}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}
