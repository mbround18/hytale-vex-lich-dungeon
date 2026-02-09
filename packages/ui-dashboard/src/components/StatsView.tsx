import React, { useEffect, useMemo, useState } from 'react';
import type { Metrics, ServerStats } from '../types';
import { Badge, DataTable, HeroBanner, KpiCard, MetricList, PlayerRow, SectionTitle, TrackingCard } from 'ui-shared/components';
import { streamStatus$ } from '../state/dashboardBus';

export type StatsViewProps = {
  metrics: Metrics;
  playerRoster: any[];
  instanceList: any[];
  portalList: any[];
  serverStats: ServerStats | null;
  worldMetadata: Array<{ name: string; playerCount?: number }> | null;
};

const formatUptime = (ms?: number) => {
  if (!ms) return '—';
  const totalSeconds = Math.floor(ms / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return `${hours}h ${minutes}m ${seconds}s`;
};

const formatMB = (bytes?: number) => {
  if (bytes == null) return '—';
  return `${Math.max(0, Math.round(bytes / 1024 / 1024))} MB`;
};

const formatDuration = (ms?: number) => {
  if (ms == null || ms < 0) return '—';
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}m ${seconds}s`;
};

const formatSeconds = (seconds?: number) => {
  if (seconds == null || Number.isNaN(seconds)) return '—';
  const clamped = Math.max(0, Math.floor(seconds));
  const mins = Math.floor(clamped / 60);
  const secs = clamped % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const StatsView: React.FC<StatsViewProps> = ({ metrics, playerRoster, instanceList, portalList, serverStats, worldMetadata }) => {
  const [isConnected, setIsConnected] = useState(false);
  const [timelineWindow, setTimelineWindow] = useState<'15m' | '30m' | '60m' | 'all'>('60m');

  useEffect(() => {
    const sub = streamStatus$.subscribe((status) => {
      setIsConnected(status.connected);
    });
    return () => sub.unsubscribe();
  }, []);

  const worlds = serverStats?.worlds || [];
  const mergedWorlds = useMemo(() => {
    const merged = new Map<string, { name: string; players?: number; loaded_chunks?: number }>();
    worlds.forEach((world) => {
      merged.set(world.name, { ...world });
    });
    (worldMetadata || []).forEach((world) => {
      const existing = merged.get(world.name);
      merged.set(world.name, {
        name: world.name,
        players: existing?.players ?? world.playerCount ?? 0,
        loaded_chunks: existing?.loaded_chunks
      });
    });
    return Array.from(merged.values());
  }, [worldMetadata, worlds]);
  const memoryUsed = serverStats
    ? serverStats.system.memory_total - serverStats.system.memory_free
    : undefined;

  const timelineData = useMemo(() => {
    const now = Date.now();
    const windowMs = timelineWindow === '15m' ? 15 * 60 * 1000
      : timelineWindow === '30m' ? 30 * 60 * 1000
      : timelineWindow === '60m' ? 60 * 60 * 1000
      : null;
    const windowStart = windowMs ? now - windowMs : null;
    const rows = (instanceList || [])
      .map((inst: any) => {
        const start = inst.startedAt ? Date.parse(inst.startedAt) : NaN;
        const teardownStart = inst.teardownStartedAt ? Date.parse(inst.teardownStartedAt) : NaN;
        const teardownEnd = inst.teardownCompletedAt ? Date.parse(inst.teardownCompletedAt) : NaN;
        const end = Number.isNaN(teardownEnd)
          ? (Number.isNaN(teardownStart) ? now : now)
          : teardownEnd;
        return {
          ...inst,
          start,
          end
        };
      })
      .filter((row) => !Number.isNaN(row.start))
      .filter((row) => {
        if (!windowStart) return true;
        return row.end >= windowStart;
      });

    const minStart = rows.length ? Math.min(...rows.map(r => r.start)) : now;
    const maxEnd = rows.length ? Math.max(...rows.map(r => r.end)) : now;
    const span = Math.max(1, maxEnd - minStart);

    return rows.map((row) => ({
      ...row,
      offsetPct: ((row.start - minStart) / span) * 100,
      widthPct: Math.max(1, ((row.end - row.start) / span) * 100),
      durationMs: row.end - row.start
    }));
  }, [instanceList, timelineWindow]);

  const portalSummary = useMemo(() => {
    const total = metrics.portalStats.total || 0;
    const entered = metrics.portalStats.entered || 0;
    const entryRate = total > 0 ? Math.round((entered / total) * 100) : 0;
    const now = Date.now();
    let urgent = 0;
    let warning = 0;
    portalList.forEach((portal: any) => {
      if (!portal.expiresAt || portal.status !== 'active') return;
      const delta = Date.parse(portal.expiresAt) - now;
      if (delta <= 60000) {
        urgent += 1;
      } else if (delta <= 300000) {
        warning += 1;
      }
    });
    return { total, entered, entryRate, urgent, warning };
  }, [metrics.portalStats, portalList]);

  return (
    <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
      <div className="max-w-7xl mx-auto space-y-8">
        <HeroBanner
          badge={isConnected ? 'Live Tracking Matrix' : 'Signal Offline'}
          title="Vex Instance Telemetry Command"
          subtitle="Track every player movement, instance lifecycle, and portal transition with continuous event reconstruction."
          right={(
            <div className="grid grid-cols-2 gap-4">
              <KpiCard title="Active Instances" value={metrics.instanceStats.active} />
              <KpiCard title="Players Tracked" value={metrics.playerStats.total} color="text-[#fbbf24]" />
              <KpiCard title="Active Portals" value={metrics.portalStats.active} color="text-[#4ade80]" />
              <KpiCard title="Events / 60s" value={metrics.instanceStats.recentEvents} />
            </div>
          )}
        />

        <section>
          <TrackingCard className="p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <SectionTitle>System Telemetry</SectionTitle>
                <p className="section-subtitle mt-2">Live stats from /api/stats.</p>
              </div>
              <Badge variant={isConnected ? 'green' : 'default'}>{isConnected ? 'Live Link' : 'Offline'}</Badge>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <MetricList
                items={[
                  { label: 'Uptime', value: formatUptime(serverStats?.system.uptime_ms) },
                  { label: 'Threads', value: serverStats?.system.threads_active ?? '—' },
                  { label: 'Memory Used', value: formatMB(memoryUsed) },
                  { label: 'Memory Total', value: formatMB(serverStats?.system.memory_total) }
                ]}
              />
              <MetricList
                items={[
                  { label: 'Event Buffer', value: serverStats?.events.buffer_size ?? '—' },
                  { label: 'Event Types', value: serverStats?.events.registered_types ?? '—' },
                  { label: 'Clients', value: serverStats?.events.clients_connected ?? '—' }
                ]}
              />
            </div>

            <div className="mt-6 overflow-x-auto">
              <DataTable
                columns={[
                  { key: 'name', label: 'World', render: (world: any) => <span className="font-semibold text-white">{world.name}</span> },
                  { key: 'players', label: 'Players', render: (world: any) => world.players ?? 0 },
                  { key: 'loaded_chunks', label: 'Loaded Chunks', render: (world: any) => world.loaded_chunks ?? '—' }
                ]}
                rows={mergedWorlds}
                getRowKey={(world: any) => world.name}
                emptyMessage="No world stats reported."
              />
            </div>
          </TrackingCard>
        </section>

        <section>
          <TrackingCard className="p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <SectionTitle>Player Roster</SectionTitle>
                <p className="section-subtitle mt-2">Current positions, world assignments, and last portal seen.</p>
              </div>
              <Badge variant="green">Live Roster</Badge>
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
              {playerRoster.map((p: any) => (
                <div key={p.uuid || p.playerId || p.name} className="space-y-2">
                  <PlayerRow
                    name={p.name}
                    world={p.world}
                    room={p.roomKey || '—'}
                    status={p.status || 'active'}
                  />
                  <MetricList
                    items={[
                      { label: 'Portal', value: p.lastPortalId ? p.lastPortalId.slice(0, 6) : 'None' },
                      { label: 'Last Seen', value: p.lastSeenAt ? new Date(p.lastSeenAt).toLocaleTimeString() : 'Awaiting' }
                    ]}
                  />
                </div>
              ))}
              {playerRoster.length === 0 && (
                <div className="text-center text-xs text-[#5f2b84]">No players tracked.</div>
              )}
            </div>
          </TrackingCard>
        </section>

        <section>
          <TrackingCard className="p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <SectionTitle>Instance Timeline</SectionTitle>
                <p className="section-subtitle mt-2">Lifecycle, player load, and generated topology.</p>
              </div>
              <Badge>Lifecycle Monitor</Badge>
            </div>
            <div className="space-y-4">
              <div className="flex items-center gap-2 text-[10px] font-mono text-gray-500">
                <span>Window:</span>
                {(['15m', '30m', '60m', 'all'] as const).map((range) => (
                  <button
                    key={range}
                    onClick={() => setTimelineWindow(range)}
                    className={`px-2 py-1 rounded border transition-all ${
                      timelineWindow === range
                        ? 'bg-violet-500/20 border-violet-500/40 text-violet-200'
                        : 'border-white/10 hover:border-white/30 text-gray-500'
                    }`}
                  >
                    {range}
                  </button>
                ))}
              </div>
              <div className="rounded-xl border border-white/5 bg-black/30 p-4">
                {timelineData.length === 0 && (
                  <div className="text-xs text-gray-500">No instance timelines available.</div>
                )}
                {timelineData.map((inst: any) => (
                  <div key={inst.name} className="flex items-center gap-4 py-2">
                    <div className="w-48 text-xs font-semibold text-white truncate">{inst.name}</div>
                    <div className="flex-1 relative h-3 bg-white/5 rounded-full overflow-hidden">
                      <div
                        className={`absolute h-full rounded-full ${
                          inst.status === 'active'
                            ? 'bg-gradient-to-r from-emerald-400 to-emerald-600'
                            : inst.status === 'teardown'
                              ? 'bg-gradient-to-r from-amber-400 to-amber-600'
                              : 'bg-gradient-to-r from-slate-500 to-slate-700'
                        }`}
                        style={{ left: `${inst.offsetPct}%`, width: `${inst.widthPct}%` }}
                      />
                    </div>
                    <div className="w-24 text-[10px] font-mono text-gray-400">
                      {formatDuration(inst.durationMs)}
                    </div>
                  </div>
                ))}
              </div>
              <DataTable
                columns={[
                  { key: 'name', label: 'Instance', render: (inst: any) => <span className="font-semibold text-white">{inst.name}</span> },
                  {
                    key: 'status',
                    label: 'Status',
                    render: (inst: any) => (
                      <Badge variant={inst.status === 'active' ? 'green' : inst.status === 'teardown' ? 'gold' : 'default'}>
                        {inst.status || 'active'}
                      </Badge>
                    )
                  },
                  { key: 'rooms', label: 'Rooms', render: (inst: any) => Object.keys(inst.rooms || {}).length },
                  { key: 'players', label: 'Players', render: (inst: any) => (inst.players ? inst.players.size : 0) },
                  { key: 'startedAt', label: 'Started', render: (inst: any) => (inst.startedAt ? new Date(inst.startedAt).toLocaleTimeString() : '—') }
                ]}
                rows={instanceList}
                getRowKey={(inst: any) => inst.name}
                emptyMessage="No active instances reported."
              />
            </div>
          </TrackingCard>
        </section>

        <section>
          <TrackingCard className="p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <SectionTitle>Portal Ledger</SectionTitle>
                <p className="section-subtitle mt-2">Creation, entry, and expiry visibility per portal ID.</p>
              </div>
              <Badge variant="gold">Gateway Watch</Badge>
            </div>
            <div className="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-6">
              <div className="p-3 rounded-lg border border-white/5 bg-black/30">
                <div className="text-[10px] uppercase text-gray-500 font-mono">Total Portals</div>
                <div className="text-xl font-semibold text-white">{portalSummary.total}</div>
              </div>
              <div className="p-3 rounded-lg border border-white/5 bg-black/30">
                <div className="text-[10px] uppercase text-gray-500 font-mono">Entries</div>
                <div className="text-xl font-semibold text-emerald-300">{portalSummary.entered}</div>
                <div className="text-[10px] text-gray-500">Conversion {portalSummary.entryRate}%</div>
              </div>
              <div className="p-3 rounded-lg border border-white/5 bg-black/30">
                <div className="text-[10px] uppercase text-gray-500 font-mono">Expiring &lt; 5m</div>
                <div className="text-xl font-semibold text-amber-300">{portalSummary.warning}</div>
              </div>
              <div className="p-3 rounded-lg border border-white/5 bg-black/30">
                <div className="text-[10px] uppercase text-gray-500 font-mono">Expiring &lt; 1m</div>
                <div className="text-xl font-semibold text-rose-300">{portalSummary.urgent}</div>
              </div>
            </div>
            <div className="overflow-x-auto">
              <DataTable
                columns={[
                  { key: 'id', label: 'Portal', render: (portal: any) => <span className="font-semibold text-white">{portal.id.slice(0, 8)}</span> },
                  {
                    key: 'status',
                    label: 'Status',
                    render: (portal: any) => (
                      <Badge variant={portal.status === 'active' ? 'green' : portal.status === 'expired' ? 'gold' : 'default'}>
                        {portal.status}
                      </Badge>
                    )
                  },
                  { key: 'world', label: 'World', render: (portal: any) => portal.world },
                  { key: 'enterCount', label: 'Entries', render: (portal: any) => portal.enterCount || 0 },
                  {
                    key: 'urgency',
                    label: 'Urgency',
                    render: (portal: any) => {
                      if (!portal.expiresAt || portal.status !== 'active') return '—';
                      const deltaMs = Date.parse(portal.expiresAt) - Date.now();
                      const seconds = Math.max(0, deltaMs / 1000);
                      const tone = seconds <= 60 ? 'text-rose-300' : seconds <= 300 ? 'text-amber-300' : 'text-emerald-300';
                      return <span className={`font-mono text-[10px] ${tone}`}>{formatSeconds(seconds)}</span>;
                    }
                  },
                  { key: 'expiresAt', label: 'Expires', render: (portal: any) => (portal.expiresAt ? new Date(portal.expiresAt).toLocaleTimeString() : '—') }
                ]}
                rows={portalList}
                getRowKey={(portal: any) => portal.id}
                emptyMessage="No portal telemetry loaded."
              />
            </div>
          </TrackingCard>
        </section>
      </div>
    </div>
  );
};

export default StatsView;
