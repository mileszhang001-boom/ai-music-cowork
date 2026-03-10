const NeteaseCloudMusicApi = require('NeteaseCloudMusicApi')

const port = process.env.PORT || 3000

NeteaseCloudMusicApi.start({ port })

console.log(`NeteaseCloudMusicApi server running at http://localhost:${port}`)
