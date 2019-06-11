package proxy

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
)

type BrowseWebSite interface {
	Browse(domain string) bool
}

type User struct {
	Name string
}

func (user User) Browse(domain string) bool {
	fmt.Printf("%s can access the web site", user.Name)
	if resp, err := http.Get(domain); err == nil {
		defer resp.Body.Close()
		content, _ := ioutil.ReadAll(resp.Body)
		fmt.Println(string(content))
	}

	return true
}

type Proxy struct {
	user User
}

func (proxy Proxy) Browse(domain string) bool {
	if !strings.HasPrefix(domain, "https") {
		fmt.Printf("%s 该站点不安全请使用https 方式访问 : %s", proxy.user.Name, domain)
		return false
	}

	fmt.Printf("%s 可以访问该站点 : %s", proxy.user.Name, domain)
	proxy.user.Browse(domain)
	return true
}
