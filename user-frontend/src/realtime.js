import { Client } from '@stomp/stompjs'
function userId(token) { try { return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).userId } catch { return null } }
export function connectUser(onMessage, onStatus = () => {}) {
  const token = localStorage.getItem('user_token'), id = userId(token); if (!token || !id) return null
  const client = new Client({ brokerURL: `ws://${location.hostname}:8080/ws`, connectHeaders: { Authorization: `Bearer ${token}` }, reconnectDelay: 3000 })
  client.onConnect = () => { onStatus(true); client.subscribe(`/topic/user/${id}`, message => onMessage(JSON.parse(message.body))) }
  client.onWebSocketClose = () => onStatus(false)
  client.onStompError = () => onStatus(false)
  client.activate(); return client
}
