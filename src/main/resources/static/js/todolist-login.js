var app = new Vue({
    el: '#content',
    data: {},
    computed: {
        errorStatus: function() {
            console.log(window.location.href)
            return window.location.href.endsWith('error=true')
        }
    }
})
