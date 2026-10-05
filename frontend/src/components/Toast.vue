<script setup>defineProps({message:String,type:String})</script><template><Transition name="toast"><div v-if="message" class="toast" :class="type">{{message}}</div></Transition></template>
