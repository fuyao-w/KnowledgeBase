package decorator

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
)

/*
现在有一个访问 http web 站点的功能，为他添加一个功能使其通过https 访问站点
*/
type browseWebSite interface {
	Browse(domain string)
}

type safeBrowseWebSite struct {
	browseWebSite
	safe bool
}

type user struct {
	Name string
}

func (b safeBrowseWebSite) Browse(domain string) {
	fmt.Println("安全访问")
	if b.safe {
		domain = strings.Replace(domain, "http", "https", 1)
	}
	if resp, err := http.Get(domain); err == nil {
		defer resp.Body.Close()
		content, _ := ioutil.ReadAll(resp.Body)
		fmt.Println(string(content))
	}
}
func (user) Browse(domain string) {
	if resp, err := http.Get(domain); err == nil {
		defer resp.Body.Close()
		content, _ := ioutil.ReadAll(resp.Body)
		fmt.Println(string(content))
	}
}

func wrapBrowseWebSite(b browseWebSite, safe bool) browseWebSite {
	return safeBrowseWebSite{
		b, safe,
	}
}
