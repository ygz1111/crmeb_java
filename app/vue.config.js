module.exports = {
	productionSourceMap: true,
	devServer: {
		port: 8080,
		proxy: {
			'/crmebimage': {
				target: 'http://localhost:20510',
				changeOrigin: true,
			},
			'/uploadf': {
				target: 'http://localhost:20510',
				changeOrigin: true,
			}
		},
	},
	configureWebpack: config => {
		if (process.env.NODE_ENV === 'production') {
			config.optimization.minimizer[0].options.terserOptions.compress.warnings = false
			config.optimization.minimizer[0].options.terserOptions.compress.drop_console = true
			config.optimization.minimizer[0].options.terserOptions.compress.drop_debugger = true
			config.optimization.minimizer[0].options.terserOptions.compress.pure_funcs = ['console.log']
		}
	}
}