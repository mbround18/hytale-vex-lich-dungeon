import * as d3 from "d3";

type RoomStats = {
  entities?: number;
  kills?: number;
  order?: number;
  generatedAt?: string;
};

type InstanceStats = {
  entityCount?: number;
  killCount?: number;
  roomCount?: number;
};

type RoomStyle = {
  fillColor: string;
  strokeColor: string;
  strokeOpacity: number;
};

type InstanceLike = {
  name: string;
  isVexDungeon?: boolean;
  maxRoomOrder?: number;
  stats?: InstanceStats;
};

type RoomLike = {
  stats?: RoomStats;
};

type InstanceHandler = {
  id: string;
  matches: (name: string) => boolean;
  getRoomStyle: (
    room: RoomLike,
    inst: InstanceLike,
    defaults: RoomStyle,
  ) => RoomStyle;
};

const defaultHandler: InstanceHandler = {
  id: "default",
  matches: () => true,
  getRoomStyle: (room, inst, defaults) => {
    const maxOrder = inst.maxRoomOrder ?? 0;
    const order = room.stats?.order ?? 0;
    if (maxOrder && order) {
      const t = Math.min(1, Math.max(0, order / maxOrder));
      const fillColor = d3.interpolateRgb("#0b1220", "#1e3a8a")(t);
      const strokeColor = d3.interpolateRgb("#475569", "#38bdf8")(t);
      return {
        ...defaults,
        fillColor,
        strokeColor,
        strokeOpacity: Math.max(defaults.strokeOpacity, 0.5),
      };
    }
    return defaults;
  },
};

const vexHandler: InstanceHandler = {
  id: "vex",
  matches: (name) => name.toLowerCase().includes("vex_the_lich_dungeon"),
  getRoomStyle: (room, inst, defaults) => {
    const maxOrder = inst.maxRoomOrder ?? 0;
    const order = room.stats?.order ?? 0;
    if (!maxOrder || !order) return defaults;
    const t = Math.min(1, Math.max(0, order / maxOrder));
    const fillColor = d3.interpolateRgb("#1b0b12", "#7f1d1d")(t);
    const strokeColor = d3.interpolateRgb("#f43f5e", "#fbbf24")(t);
    return {
      ...defaults,
      fillColor,
      strokeColor,
      strokeOpacity: Math.max(defaults.strokeOpacity, 0.6),
    };
  },
};

const handlers: InstanceHandler[] = [vexHandler, defaultHandler];

const applyClearState = (room: RoomLike, style: RoomStyle) => {
  const entities = room.stats?.entities ?? 0;
  const kills = room.stats?.kills ?? 0;
  const cleared = entities > 0 && kills >= entities;
  if (!cleared) return style;
  return {
    ...style,
    fillColor: d3.interpolateRgb(style.fillColor, "#0f172a")(0.65),
    strokeColor: d3.interpolateRgb(style.strokeColor, "#64748b")(0.6),
    strokeOpacity: Math.max(0.25, style.strokeOpacity * 0.6),
  };
};

export const resolveInstanceHandler = (name: string) =>
  handlers.find((handler) => handler.matches(name)) || defaultHandler;

export const getStyledRoom = (
  room: RoomLike,
  inst: InstanceLike,
  defaults: RoomStyle,
) => {
  const handler = resolveInstanceHandler(inst.name);
  const base = handler.getRoomStyle(room, inst, defaults);
  return applyClearState(room, base);
};
