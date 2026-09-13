import type { Task } from '@/types/game'

function rot13(value: string): string {
  return value.replace(/[a-z]/gi, (char) => {
    const code = char.charCodeAt(0)
    const base = code >= 97 ? 97 : 65
    return String.fromCharCode(((code - base + 13) % 26) + base)
  })
}

// C0/C1 control characters, except tab, newline and carriage return.
const CONTROL_CHARS = /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f-\u009f]/

function fromBase64(value: string): string | null {
  try {
    const standard = value.replace(/-/g, '+').replace(/_/g, '/')
    const padded = standard.padEnd(standard.length + ((4 - (standard.length % 4)) % 4), '=')
    const binary = atob(padded)
    const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
    const decoded = new TextDecoder('utf-8', { fatal: true }).decode(bytes)
    if (!CONTROL_CHARS.test(decoded)) {
      return decoded
    }
  } catch {
    return null
  }
  return null
}

export function decodeField(value: string): string {
  return fromBase64(value) ?? rot13(value)
}

export function decodeTask(task: Task): Task {
  if (!task.encrypted) return task
  return {
    ...task,
    adId: decodeField(task.adId),
    message: decodeField(task.message),
    probability: decodeField(task.probability),
  }
}
