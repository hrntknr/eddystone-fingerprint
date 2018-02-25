const express = require('express')
const router = express.Router()
const dataStore = require('../lib/DataStore')
const logger = require('../lib/logger')

router.get('/', async(req, res)=>{
  res.render('index', {registedPos: dataStore.getRegistedPos()})
})

router.post('/rssi', async(req, res)=>{
  const {uid, rssi, androidId} = req.body
  dataStore.rssi(uid, rssi, androidId)
  res.json({})
})

router.post('/registerPos', async(req, res)=>{
  const {x, y, androidId} = req.body
  logger.system.debug(`registerPos: ${x} ${y}`)
  dataStore.registerPos(x, y, androidId)
  res.json({})
})

module.exports = router