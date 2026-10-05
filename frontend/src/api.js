export async function api(path,options={}){
  const token=localStorage.getItem('admin_token')
  const headers={...(options.body?{'Content-Type':'application/json'}:{}),...options.headers}
  if(token)headers.Authorization=`Bearer ${token}`
  const response=await fetch(path,{...options,headers})
  if(response.status===401||response.status===403){localStorage.removeItem('admin_token');location.href='/login';throw new Error('Session expired')}
  const result=await response.json()
  if(!response.ok||result.code!==1)throw new Error(result.msg||`Request failed (${response.status})`)
  return result.data
}
export const json=data=>JSON.stringify(data)
