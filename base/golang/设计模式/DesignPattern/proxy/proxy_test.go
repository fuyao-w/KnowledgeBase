package proxy

import (
	"testing"
)

func TestProxy(t *testing.T) {
	var url = "https://www.baidu.com"

	var user = Proxy{User{"wfy"}}
	user.Browse(url)
}
