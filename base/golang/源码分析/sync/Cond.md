```go
package main

import (
	"fmt"
	"os"
	"os/signal"
	"sync"
	"syscall"
)
/*
	生产消费
*/
var count int
var c = sync.NewCond(&sync.Mutex{})
func main() {

	for x := 0; x < 2; x++ {
		go func(add int) {
			for i := 0; i < 10; i++ {
				c.L.Lock()
				if add == 0 {
					count++
				} else {
					count--
				}

				fmt.Printf("count :%d\n", count)
				c.Signal()
				c.Wait()
				c.L.Unlock()
			}
		}(x)
	}
	v := make(chan os.Signal, 1)
	signal.Notify(v, syscall.SIGINT, syscall.SIGINFO)
	done := make(chan int, 1)
	for {
		end := <-v
		fmt.Println("wfy")
		fmt.Println(end)
		done <- 1
		break
	}
	<-done
}

```