package decorator

import (
	"testing"
)

func TestDecotator(t *testing.T) {
	var user browseWebSite = user{"wfy"}
	user = wrapBrowseWebSite(user, true)
	user.Browse("http://www.baidu.com")
}
