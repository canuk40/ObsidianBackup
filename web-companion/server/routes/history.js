import express from 'express';
const router = express.Router();

const mockHistory = [
  {
    id: '1',
    action: 'Full Backup',
    status: 'success',
    timestamp: new Date('2024-02-08T10:30:00').toISOString(),
    description: 'Completed full backup of all data',
    details: { files: 234, size: 157286400, duration: 45 }
  },
  {
    id: '2',
    action: 'Incremental Backup',
    status: 'success',
    timestamp: new Date('2024-02-07T10:30:00').toISOString(),
    description: 'Completed incremental backup',
    details: { files: 67, size: 47185920, duration: 12 }
  }
];

router.get('/', (req, res) => {
  const { limit = 50 } = req.query;
  res.json(mockHistory.slice(0, parseInt(limit)));
});

router.get('/:id', (req, res) => {
  const entry = mockHistory.find(h => h.id === req.params.id);
  if (!entry) {
    return res.status(404).json({ error: 'History entry not found' });
  }
  res.json(entry);
});

router.get('/stats', (req, res) => {
  const { period = '30d' } = req.query;
  
  res.json({
    totalBackups: mockHistory.length,
    successRate: 100,
    avgDuration: 28,
    timeline: [
      { date: '2024-02-01', count: 3 },
      { date: '2024-02-02', count: 2 },
      { date: '2024-02-03', count: 4 },
      { date: '2024-02-04', count: 3 },
      { date: '2024-02-05', count: 5 }
    ],
    sizeTimeline: [
      { date: '2024-02-01', size: 150 },
      { date: '2024-02-02', size: 145 },
      { date: '2024-02-03', size: 160 },
      { date: '2024-02-04', size: 155 },
      { date: '2024-02-05', size: 170 }
    ]
  });
});

export default router;
