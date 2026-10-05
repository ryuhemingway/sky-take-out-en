<script setup>
import { ref, onMounted } from 'vue'
import { api, json } from '../api'
import Modal from '../components/Modal.vue'
import { Plus, RefreshCw, Pencil, Trash2 } from 'lucide-vue-next'

const rows = ref([])
const error = ref('')
const blank = () => ({ code: '', name: '', discountAmount: 5, thresholdAmount: 30, totalStock: 100,
  startTime: new Date().toISOString().slice(0, 16), endTime: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 16) })
const form = ref(blank()), show = ref(false)

async function load() {
  try { rows.value = await api('/api/admin/coupon/page'); error.value = '' }
  catch (e) { error.value = e.message }
}
function open(r) { form.value = r ? { ...r, startTime: r.startTime?.slice(0, 16), endTime: r.endTime?.slice(0, 16) } : blank(); show.value = true }
async function save() {
  try {
    await api('/api/admin/coupon', { method: form.value.id ? 'PUT' : 'POST', body: json(form.value) })
    show.value = false; error.value = ''; await load()
  } catch (e) { error.value = e.message }
}
async function remove(r) {
  if (!confirm(`Delete coupon "${r.code}"?`)) return
  try { await api(`/api/admin/coupon/${r.id}`, { method: 'DELETE' }); error.value = ''; await load() } catch (e) { error.value = e.message }
}
async function toggle(row) {
  await api(`/api/admin/coupon/${row.id}/status/${row.status ? 0 : 1}`, { method: 'PUT' }); await load()
}
onMounted(load)
</script>

<template>
  <div class="page-head"><div><h1>Coupons</h1><p>Create spend-and-save promotions and manage stock</p></div><div><button class="secondary" @click="load"><RefreshCw :size="17"/>Refresh</button> <button class="primary" @click="open()"><Plus :size="16"/>Create Coupon</button></div></div>
  <p v-if="error" class="error">{{ error }}</p>
  <div class="table-wrap"><table><thead><tr><th>Code</th><th>Name</th><th>Rule</th><th>Stock</th><th>Validity</th><th>Status</th><th>Actions</th></tr></thead>
  <tbody><tr v-for="r in rows" :key="r.id"><td><strong>{{r.code}}</strong></td><td>{{r.name}}</td><td>Spend ¥{{r.thresholdAmount}} save ¥{{r.discountAmount}}</td><td>{{r.remainingStock}} / {{r.totalStock}}</td><td>{{r.startTime?.replace('T',' ')}}<small>to {{r.endTime?.replace('T',' ')}}</small></td><td><span class="status" :class="r.status?'s5':'s6'">{{r.status?'Enabled':'Disabled'}}</span></td><td><button class="text-action" @click="toggle(r)">{{r.status?'Disable':'Enable'}}</button><button class="text-action" @click="open(r)"><Pencil :size="14"/> Edit</button><button class="text-action" @click="remove(r)"><Trash2 :size="14"/> Delete</button></td></tr></tbody></table></div>
  <Modal v-if="show" :title="form.id ? 'Edit Coupon' : 'Create Coupon'" @close="show = false"><form class="form-grid" @submit.prevent="save">
    <label>Code<input v-model="form.code" required placeholder="e.g. SAVE20"></label><label>Name<input v-model="form.name" required></label>
    <label>Minimum spend<input v-model.number="form.thresholdAmount" type="number" min="0" required></label><label>Discount amount<input v-model.number="form.discountAmount" type="number" min="0.01" step="0.01" required></label>
    <label>Quantity<input v-model.number="form.totalStock" type="number" min="1" required></label><label>Start<input v-model="form.startTime" type="datetime-local" required></label><label>End<input v-model="form.endTime" type="datetime-local" required></label>
    <footer><button type="button" class="secondary" @click="show = false">Cancel</button><button class="primary">Save</button></footer></form></Modal>
</template>
