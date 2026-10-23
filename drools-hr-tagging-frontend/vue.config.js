module.exports = {
  devServer: {
    // 前端独立端口，避免与后端 8080 冲突；后端 CORS 已放行
    port: 9528,
    open: true,
  },
}
