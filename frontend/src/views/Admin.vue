<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { LayoutDashboard, Tags, UtensilsCrossed, PackageOpen, ClipboardList, LogOut, Store, Menu, X, Bike, TicketPercent, Bell, Wifi, WifiOff } from 'lucide-vue-next'
import { api } from '../api'
import Overview from './Overview.vue'; import Categories from './Categories.vue'; import Dishes from './Dishes.vue'
import Setmeals from './Setmeals.vue'; import Orders from './Orders.vue'; import Coupons from './Coupons.vue'; import Delivery from './Delivery.vue'
import { connectAdmin } from '../realtime'

const router = useRouter(), active = ref('overview'), open = ref(false), shop = ref(1), notice = ref(''), refreshKey = ref(0)
const connected = ref(false), showNotices = ref(false), notifications = ref([])
let socket
const items = [['overview','Overview',LayoutDashboard],['categories','Categories',Tags],['dishes','Dishes',UtensilsCrossed],['setmeals','Set Meals',PackageOpen],['orders','Orders',ClipboardList],['coupons','Coupons',TicketPercent],['delivery','Riders & Delivery',Bike]]
const current = computed(() => ({ overview:Overview,categories:Categories,dishes:Dishes,setmeals:Setmeals,orders:Orders,coupons:Coupons,delivery:Delivery })[active.value])
const unread = computed(() => notifications.value.filter(item => !item.read).length)
async function getShop(){try{shop.value=(await api('/api/shop/status')).status}catch{}}
async function toggleShop(){const next=shop.value?0:1;await api(`/api/shop/status?status=${next}`,{method:'PUT'});shop.value=next}
function logout(){socket?.deactivate();localStorage.clear();router.push('/login')}
function toggleNotices(){showNotices.value=!showNotices.value;if(showNotices.value)notifications.value.forEach(item=>item.read=true)}
function receive(event){
  const text=({NEW_ORDER:'New order received',ORDER_PAID:'Order paid, please accept it promptly',REMINDER:'Customer sent an order reminder'})[event.type]||'Order status updated'
  notifications.value.unshift({text,time:new Date().toLocaleTimeString(),read:false,type:event.type});notifications.value=notifications.value.slice(0,20)
  notice.value=text;active.value='orders';refreshKey.value++;setTimeout(()=>notice.value='',3500)
}
onMounted(()=>{getShop();socket=connectAdmin(receive,value=>connected.value=value)})
onUnmounted(()=>socket?.deactivate())
</script>
<template><div class="admin-shell"><aside :class="{open}"><div class="side-brand"><span><Store :size="23"/></span><strong>Sky Take-Out</strong><button class="icon-btn mobile" @click="open=false"><X/></button></div><nav><button v-for="item in items" :key="item[0]" :class="{active:active===item[0]}" @click="active=item[0];open=false"><component :is="item[2]" :size="19"/>{{item[1]}}</button></nav><div class="side-foot"><button @click="logout"><LogOut :size="18"/>Log Out</button></div></aside><main class="workspace"><header class="topbar"><button class="icon-btn mobile" @click="open=true"><Menu/></button><div><span class="top-label">Shop Management</span><strong>{{items.find(i=>i[0]===active)?.[1]}}</strong></div><div class="top-actions"><span class="socket-state" :class="{offline:!connected}"><Wifi v-if="connected" :size="15"/><WifiOff v-else :size="15"/>{{connected?'Live alerts connected':'Connecting...'}}</span><div class="notice-wrap"><button class="notice-button" title="Live Notifications" @click="toggleNotices"><Bell :size="19"/><b v-if="unread">{{unread}}</b></button><section v-if="showNotices" class="notice-panel"><header><strong>Live Notifications</strong><small>{{connected?'WebSocket online':'Reconnecting'}}</small></header><p v-if="!notifications.length">No new messages. New orders and reminders will appear here.</p><button v-for="item in notifications" :key="item.time+item.text" @click="active='orders';showNotices=false"><span>{{item.text}}</span><small>{{item.time}}</small></button></section></div><button class="shop-switch" :class="{closed:!shop}" @click="toggleShop"><i></i>{{shop?'Open':'Closed'}}</button></div></header><section class="content"><component :is="current" :key="active==='orders'?refreshKey:active"/></section></main><div v-if="notice" class="toast live-toast"><Bell :size="17"/>{{notice}}</div></div></template>
