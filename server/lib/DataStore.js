const {EventEmitter} = require('events')

const data = {}
const registedPos = []

const ee = new EventEmitter()

function getRegistedPos() {
  return registedPos
}

/**
 *
 * @param {*} uid
 * @param {*} rssi
 * @param {*} androidId
 */
function rssi(uid, rssi, androidId) {
  if(!data[androidId]) {
    data[androidId] = {}
  }
  data[androidId][uid] = rssi
  ;(()=>{
    const _k = 1
    const _registedPos = JSON.parse(JSON.stringify(registedPos))
    const k = _k>_registedPos.length?_registedPos.length:_k
    _registedPos.forEach((element)=>{
      let d = 0
      for(let uid in element.rssi) {
        if(data[androidId][uid]!=null) {
          d += Math.abs(data[androidId][uid] - element.rssi[uid])
        }else {
          d += 10
        }
      }
      element.d = d
    })
    _registedPos.sort((a, b)=>{
      return a.d - b.d
    })
    const total_d = _registedPos.slice(0, k).reduce((t, e)=>{return t+e.d}, 0)
    let x = 0
    let y = 0
    for(let i=0;i<k;i++) {
      x += _registedPos[i].x * _registedPos[i].d / total_d
      y += _registedPos[i].y * _registedPos[i].d / total_d
    }
    ee.emit('pos', {x, y, androidId})
  })()
}

/**
 *
 * @param {*} x
 * @param {*} y
 * @param {*} androidId
 */
function registerPos(x, y, androidId) {
  const posInfo = {
    x, y,
    rssi: Object.assign({}, data[androidId])
  }
  registedPos.push(posInfo)
  ;(()=>{
    ee.emit('newPos', posInfo)
  })()
}

module.exports = {
  rssi,
  registerPos,
  ee,
  getRegistedPos
}