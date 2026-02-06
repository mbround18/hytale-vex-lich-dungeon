import React from 'react';
import type { Metrics } from '../types';
import { Badge, DataTable, HeroBanner, KpiCard, MetricList, PlayerRow, SectionTitle, TrackingCard } from 'ui-shared/components';

type StatsViewProps = {
  metrics: Metrics;
  playerRoster: any[];
  instanceList: any[];
  portalList: any[];
};

export default function StatsView({ metrics, playerRoster, instanceList, portalList }: StatsViewProps) {
  return (
    <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
      <div className="max-w-7xl mx-auto space-y-8">
        <HeroBanner
          badge="Live Tracking Matrix"
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
            <div className="overflow-x-auto">
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
}
