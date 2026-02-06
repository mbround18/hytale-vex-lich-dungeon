import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';

interface MapViewProps {
  worldState: any;
  getRoomSizeForRoom: (room: any) => { w: number; h: number };
}

export default function MapView({ worldState, getRoomSizeForRoom }: MapViewProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const container = mapContainerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(entries => {
      const entry = entries[0];
      if (!entry) return;
      const { width, height } = entry.contentRect;
      setContainerSize({ width, height });
    });
    observer.observe(container);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const container = mapContainerRef.current;
    if (!container) return;
    const width = containerSize.width || container.clientWidth;
    const height = containerSize.height || container.clientHeight;
    if (!width || !height) return;
    container.innerHTML = '';

    const svg = d3.select(container).append('svg').attr('width', width).attr('height', height);
    const g = svg.append('g');

    const zoom = d3.zoom().scaleExtent([0.25, 3]).on('zoom', (event: any) => g.attr('transform', event.transform));
    svg.call(zoom as any);

    const activeInstances = Object.values(worldState.activeInstances || {});
    const archivedInstances = (worldState.archivedInstances || []).map((arch: any) => {
      const roomsObj = arch?.data?.rooms || {};
      return {
        name: arch.id || arch.name || 'archived',
        rooms: Object.values(roomsObj),
        players: new Set(),
        active: false,
        status: 'archived',
        archived: true
      };
    });
    const instanceMap = new Map<string, any>();
    activeInstances.forEach((inst: any) => {
      instanceMap.set(inst.name, { ...inst, rooms: Object.values(inst.rooms || {}) });
    });
    archivedInstances.forEach((inst: any) => {
      if (!instanceMap.has(inst.name)) {
        instanceMap.set(inst.name, inst);
      }
    });
    Object.values(worldState.players || {}).forEach((p: any) => {
      if (!p?.world) return;
      if (!instanceMap.has(p.world)) {
        instanceMap.set(p.world, { name: p.world, rooms: [], players: new Set(), active: false, status: 'unknown' });
      }
    });

    const instances = Array.from(instanceMap.values());
    if (!instances.length) {
      g.append('text')
        .attr('x', width / 2)
        .attr('y', height / 2)
        .attr('fill', '#6b7280')
        .attr('text-anchor', 'middle')
        .attr('font-size', 12)
        .text('No instances to render');
      return;
    }

    const instanceBounds = instances.map((inst: any) => {
      const rooms = Object.values(inst.rooms || {});
      const resolvedRooms = rooms.length
        ? rooms
        : [{ x: 0, z: 0, prefab: 'Empty', type: 'empty', size: { w: 1, h: 1 }, placeholder: true }];
      let minX = Infinity;
      let minZ = Infinity;
      let maxX = -Infinity;
      let maxZ = -Infinity;
      resolvedRooms.forEach((r: any) => {
        const size = getRoomSizeForRoom(r);
        const w = size?.w || 1;
        const h = size?.h || 1;
        minX = Math.min(minX, r.x);
        minZ = Math.min(minZ, r.z);
        maxX = Math.max(maxX, r.x + w - 1);
        maxZ = Math.max(maxZ, r.z + h - 1);
      });
      return {
        name: inst.name,
        rooms: resolvedRooms,
        active: inst.active,
        status: inst.status,
        archived: inst.archived,
        minX,
        minZ,
        maxX,
        maxZ,
        gridW: maxX - minX + 1,
        gridH: maxZ - minZ + 1
      };
    }).filter(Boolean) as any[];

    if (!instanceBounds.length) return;

    const gapTiles = 4;
    const maxRowTiles = 56;
    const rows: { items: any[]; width: number; height: number }[] = [];
    let row = { items: [] as any[], width: 0, height: 0 };

    instanceBounds.forEach((inst) => {
      const addWidth = (row.items.length ? gapTiles : 0) + inst.gridW;
      if (row.items.length && row.width + addWidth > maxRowTiles) {
        rows.push(row);
        row = { items: [], width: 0, height: 0 };
      }
      row.items.push(inst);
      row.width += addWidth;
      row.height = Math.max(row.height, inst.gridH);
    });
    if (row.items.length) rows.push(row);

    const totalGridW = Math.max(...rows.map(r => r.width));
    const totalGridH = rows.reduce((sum, r) => sum + r.height, 0) + gapTiles * (rows.length - 1);
    const padding = 40;
    const tileSize = Math.max(10, Math.min(40, Math.floor(Math.min((width - padding * 2) / totalGridW, (height - padding * 2) / totalGridH))));

    const gridWidthPx = totalGridW * tileSize;
    const gridHeightPx = totalGridH * tileSize;
    const originX = (width - gridWidthPx) / 2;
    const originY = (height - gridHeightPx) / 2;

    const defs = svg.append('defs');
    defs.append('pattern')
      .attr('id', 'grid-pattern')
      .attr('width', tileSize)
      .attr('height', tileSize)
      .attr('patternUnits', 'userSpaceOnUse')
      .append('path')
      .attr('d', `M ${tileSize} 0 L 0 0 0 ${tileSize}`)
      .attr('fill', 'none')
      .attr('stroke', 'rgba(255,255,255,0.05)')
      .attr('stroke-width', 1);

    g.append('rect')
      .attr('x', originX)
      .attr('y', originY)
      .attr('width', gridWidthPx)
      .attr('height', gridHeightPx)
      .attr('fill', 'url(#grid-pattern)')
      .attr('opacity', 0.6);

    const tooltip = d3.select(container)
      .append('div')
      .style('position', 'absolute')
      .style('pointer-events', 'none')
      .style('background', 'rgba(17, 12, 26, 0.95)')
      .style('border', '1px solid rgba(139, 92, 246, 0.6)')
      .style('border-radius', '8px')
      .style('padding', '8px 10px')
      .style('font-size', '11px')
      .style('color', '#f8f5ff')
      .style('box-shadow', '0 12px 24px rgba(0,0,0,0.35)')
      .style('opacity', '0');

    const hoverLayer = g.append('g').attr('class', 'hover-layer');
    const hoverRect = hoverLayer.append('rect')
      .attr('fill', 'none')
      .attr('stroke', '#f472b6')
      .attr('stroke-width', 2)
      .attr('rx', 6)
      .style('opacity', 0);

    const showTooltip = (event: any, html: string) => {
      const [x, y] = d3.pointer(event, container);
      tooltip
        .style('left', `${x + 12}px`)
        .style('top', `${y + 12}px`)
        .style('opacity', '1')
        .html(html);
    };
    const hideTooltip = () => {
      tooltip.style('opacity', '0');
      hoverRect.style('opacity', 0);
    };

    let cursorY = originY;
    rows.forEach((rowBlock) => {
      let cursorX = originX;
      rowBlock.items.forEach((inst) => {
        const instOffsetX = cursorX;
        const instOffsetY = cursorY;
        const instWidth = inst.gridW * tileSize;
        const instHeight = inst.gridH * tileSize;
        const label = inst.name.split('-').slice(0, 3).join('-');

        g.append('rect')
          .attr('x', instOffsetX)
          .attr('y', instOffsetY)
          .attr('width', instWidth)
          .attr('height', instHeight)
          .attr('fill', 'transparent')
          .attr('stroke', 'none')
          .style('pointer-events', 'all')
          .on('mouseenter', (event: any) => {
            hoverRect
              .attr('x', instOffsetX)
              .attr('y', instOffsetY)
              .attr('width', instWidth)
              .attr('height', instHeight)
              .style('opacity', 1);
            const meta = [
              `<div><strong>${inst.name}</strong></div>`,
              `<div>Status: ${inst.status || (inst.active ? 'active' : 'inactive')}</div>`,
              `<div>Rooms: ${inst.rooms.length}</div>`
            ].join('');
            showTooltip(event, meta);
          })
          .on('mouseleave', hideTooltip);

        g.append('text')
          .attr('x', instOffsetX + 6)
          .attr('y', instOffsetY - 10)
          .attr('fill', '#cdb6ff')
          .attr('font-size', 10)
          .attr('font-weight', 600)
          .text(label);

        const roomsWithMeta = inst.rooms.map((room: any) => ({
          ...room,
          __inst: inst,
          __offsetX: instOffsetX,
          __offsetY: instOffsetY
        }));

        const roomGroups = g.append('g').selectAll('g')
          .data(roomsWithMeta)
          .join('g')
          .attr('transform', (d: any) => {
            const size = getRoomSizeForRoom(d);
            const w = size?.w || 1;
            const h = size?.h || 1;
            const x = instOffsetX + (d.x - inst.minX) * tileSize;
            const y = instOffsetY + (d.z - inst.minZ) * tileSize;
            return `translate(${x},${y})`;
          });

        roomGroups.append('rect')
          .attr('width', (d: any) => (getRoomSizeForRoom(d).w || 1) * tileSize)
          .attr('height', (d: any) => (getRoomSizeForRoom(d).h || 1) * tileSize)
          .attr('rx', 6)
          .attr('fill', '#160c24')
          .attr('stroke', (d: any) => (d.x === 0 && d.z === 0 ? '#fbbf24' : '#8b5cf6'))
          .attr('stroke-width', 2);

        roomGroups.append('text')
          .attr('x', 8)
          .attr('y', 18)
          .attr('fill', '#cdb6ff')
          .attr('font-size', 10)
          .attr('font-weight', 600)
          .text((d: any) => (d.prefab || 'Room').split('/').pop());

        roomGroups.append('title')
          .text((d: any) => `${d.prefab || 'Room'} (${d.x},${d.z})`);

        roomGroups
          .on('mouseenter', (event: any, d: any) => {
            const size = getRoomSizeForRoom(d);
            const w = size?.w || 1;
            const h = size?.h || 1;
            const x = d.__offsetX + (d.x - inst.minX) * tileSize;
            const y = d.__offsetY + (d.z - inst.minZ) * tileSize;
            hoverRect
              .attr('x', x)
              .attr('y', y)
              .attr('width', w * tileSize)
              .attr('height', h * tileSize)
              .style('opacity', 1);
            const meta = [
              `<div><strong>${d.prefab || 'Room'}</strong></div>`,
              `<div>World: ${inst.name}</div>`,
              `<div>Room: ${d.x}, ${d.z}</div>`,
              `<div>Size: ${w} x ${h}</div>`
            ].join('');
            showTooltip(event, meta);
          })
          .on('mouseleave', hideTooltip);

        const players = Object.values(worldState.players).filter((p: any) => p.world === inst.name);
        const playerLayer = g.append('g');
        players.forEach((p: any) => {
          if (!p.roomKey) return;
          const [xStr, zStr] = p.roomKey.split(',');
          const x = Number.parseInt(xStr, 10);
          const z = Number.parseInt(zStr, 10);
          const roomKey = `${x},${z}`;
          const room = inst.rooms.find((r: any) => `${r.x},${r.z}` === roomKey) || inst.rooms[0];
          if (!room) return;
          const size = getRoomSizeForRoom(room);
          const w = size?.w || 1;
          const h = size?.h || 1;
          const px = instOffsetX + (room.x - inst.minX + w / 2) * tileSize;
          const py = instOffsetY + (room.z - inst.minZ + h / 2) * tileSize;
          playerLayer.append('circle')
            .attr('cx', px)
            .attr('cy', py)
            .attr('r', Math.max(3, tileSize * 0.12))
            .attr('class', 'player-dot')
            .on('mouseenter', (event: any) => {
              const size = Math.max(8, tileSize * 0.5);
              hoverRect
                .attr('x', px - size / 2)
                .attr('y', py - size / 2)
                .attr('width', size)
                .attr('height', size)
                .style('opacity', 1);
              const meta = [
                `<div><strong>${p.name || 'Player'}</strong></div>`,
                p.uuid ? `<div>UUID: ${p.uuid}</div>` : '',
                `<div>World: ${inst.name}</div>`,
                `<div>Room: ${room.x}, ${room.z}</div>`
              ].filter(Boolean).join('');
              showTooltip(event, meta);
            })
            .on('mouseleave', hideTooltip);
        });

        cursorX += inst.gridW * tileSize + gapTiles * tileSize;
      });
      cursorY += rowBlock.height * tileSize + gapTiles * tileSize;
    });

    const fitScale = Math.min(
      1,
      (width - padding * 2) / gridWidthPx,
      (height - padding * 2) / gridHeightPx
    ) * 0.9;
    const fitX = (width - gridWidthPx * fitScale) / 2;
    const fitY = (height - gridHeightPx * fitScale) / 2;
    svg.call(zoom.transform as any, d3.zoomIdentity.translate(fitX, fitY).scale(fitScale));
  }, [worldState, getRoomSizeForRoom, containerSize]);

  return (
    <div className="flex-1 flex flex-col p-8">
      <div className="mb-4 flex justify-between items-center">
        <div>
          <h3 className="section-title">Instance Topology</h3>
          <p className="section-subtitle mt-2">Live Room Reconstruction</p>
        </div>
        <div className="flex gap-4">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 border-2 border-[#fbbf24] rounded rx-1" />
            <span className="text-[9px] font-bold uppercase text-stone-500">Start Room</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 border-2 border-[#8b5cf6] rounded rx-1" />
            <span className="text-[9px] font-bold uppercase text-stone-500">Generated Room</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 bg-[#4ade80] rounded-full" />
            <span className="text-[9px] font-bold uppercase text-stone-500">Player</span>
          </div>
        </div>
      </div>
      <div ref={mapContainerRef} id="map-container" className="flex-1 bg-black/40 border border-stone-800 rounded-2xl relative overflow-hidden shadow-inner cursor-move" />
    </div>
  );
}
