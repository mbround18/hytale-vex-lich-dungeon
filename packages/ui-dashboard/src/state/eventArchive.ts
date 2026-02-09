type ArchiveRecord = {
  id: string;
  timestamp: string;
  data: any;
};

type EventLogRecord = {
  id: string;
  events: any[];
};

const DB_NAME = "VexDungeonDB";
const DB_VERSION = 2;

let dbPromise: Promise<IDBDatabase> | null = null;

const openDb = () => {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains("archives")) {
        db.createObjectStore("archives", { keyPath: "id" });
      }
      if (!db.objectStoreNames.contains("eventLogs")) {
        db.createObjectStore("eventLogs", { keyPath: "id" });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  return dbPromise;
};

const serialize = <T>(payload: T): T => {
  try {
    if (typeof structuredClone === "function") {
      return structuredClone(payload);
    }
  } catch {
    // ignore
  }
  return JSON.parse(JSON.stringify(payload)) as T;
};

const withStore = async <T>(
  storeName: "archives" | "eventLogs",
  mode: IDBTransactionMode,
  action: (store: IDBObjectStore) => Promise<T>,
) => {
  const db = await openDb();
  return new Promise<T>((resolve, reject) => {
    const tx = db.transaction(storeName, mode);
    const store = tx.objectStore(storeName);
    action(store).then(resolve).catch(reject);
  });
};

export const initArchiveDb = async () => {
  await openDb();
};

export const loadArchivesFromDb = async (): Promise<ArchiveRecord[]> => {
  return withStore(
    "archives",
    "readonly",
    (store) =>
      new Promise((resolve, reject) => {
        const req = store.getAll();
        req.onsuccess = () => resolve((req.result || []) as ArchiveRecord[]);
        req.onerror = () => reject(req.error);
      }),
  );
};

export const saveArchiveToDb = async (record: ArchiveRecord) => {
  const payload = serialize(record);
  await withStore(
    "archives",
    "readwrite",
    (store) =>
      new Promise<void>((resolve, reject) => {
        const req = store.put(payload);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      }),
  );
};

export const clearArchivesDb = async () => {
  await withStore(
    "archives",
    "readwrite",
    (store) =>
      new Promise<void>((resolve, reject) => {
        const req = store.clear();
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      }),
  );
};

export const deleteArchiveFromDb = async (archiveId: string) => {
  await withStore(
    "archives",
    "readwrite",
    (store) =>
      new Promise<void>((resolve, reject) => {
        const req = store.delete(archiveId);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      }),
  );
};

export const saveEventLogToDb = async (instanceName: string, event: any) => {
  const payload = serialize(event);
  await withStore(
    "eventLogs",
    "readwrite",
    (store) =>
      new Promise<void>((resolve, reject) => {
        const req = store.get(instanceName);
        req.onsuccess = () => {
          const existing = (req.result as EventLogRecord | undefined) || {
            id: instanceName,
            events: [],
          };
          existing.events.push(payload);
          const putReq = store.put(existing);
          putReq.onsuccess = () => resolve();
          putReq.onerror = () => reject(putReq.error);
        };
        req.onerror = () => reject(req.error);
      }),
  );
};

export const loadEventLogFromDb = async (
  instanceName: string,
): Promise<any[] | null> => {
  return withStore(
    "eventLogs",
    "readonly",
    (store) =>
      new Promise((resolve, reject) => {
        const req = store.get(instanceName);
        req.onsuccess = () =>
          resolve((req.result as EventLogRecord | undefined)?.events || null);
        req.onerror = () => reject(req.error);
      }),
  );
};

export const clearEventLogsDb = async () => {
  await withStore(
    "eventLogs",
    "readwrite",
    (store) =>
      new Promise<void>((resolve, reject) => {
        const req = store.clear();
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      }),
  );
};

export const clearArchiveStorageDb = async () => {
  await clearArchivesDb();
  await clearEventLogsDb();
};

export const deleteArchiveStorageDb = async (archiveId: string) => {
  await deleteArchiveFromDb(archiveId);
  await withStore(
    "eventLogs",
    "readwrite",
    (store) =>
      new Promise<void>((resolve, reject) => {
        const req = store.delete(archiveId);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
      }),
  );
};
