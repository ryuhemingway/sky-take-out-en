import { Client } from '@stomp/stompjs'
export function connectAdmin(onMessage, onStatus = () => {}) {
  const token = localStorage.getItem('admin_token'); if (!token) return null
  const client = new Client({ brokerURL: `ws://${location.hostname}:8080/ws`, connectHeaders: { Authorization: `Bearer ${token}` }, reconnectDelay: 3000 })
  client.onConnect = () => { onStatus(true); client.subscribe('/topic/admin', message => onMessage(JSON.parse(message.body))) }
  client.onWebSocketClose = () => onStatus(false)
  client.onStompError = () => onStatus(false)
  client.activate(); return client
}
