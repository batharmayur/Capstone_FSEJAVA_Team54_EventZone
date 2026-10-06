export async function GET() {
  return Response.json({
    status: 'ok',
    service: 'capstoneflow',
    timestamp: new Date().toISOString()
  });
}
