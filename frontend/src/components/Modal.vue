<script setup>import{X}from'lucide-vue-next';defineProps({title:String});defineEmits(['close'])</script>
<template><div class="modal-backdrop" @click.self="$emit('close')"><section class="modal"><header><h3>{{title}}</h3><button class="icon-btn" title="Close" @click="$emit('close')"><X :size="20"/></button></header><div class="modal-body"><slot/></div></section></div></template>
