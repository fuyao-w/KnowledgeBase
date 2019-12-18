## 代码1：

```go
var m *map[string]int
t.Log(m)
t.Log(json.Unmarshal([]byte(`{"code":0}`), &m))
t.Log(m)
```

### 问题：这段代码会打印出什么？

    map_test.go:34: <nil>
    map_test.go:37: <nil>
    map_test.go:38: &map[code:0]

疑问：为啥没崩溃？

答：首先需要大致了解 `go`中`map` 的底层是什么？ -> 是一个指针指向底层的`map结构`。

通过 var 关键字声明的字段，都会初始化零值。第二行打印出的 `nil`不代表引用`m`是一个空指针，`m`是一个有效的指针(打印`&m` 和`*m`可见)，但是它指向的`map`是零值。 



___



## 代码2：

```go
func TestMap(t *testing.T) {
   m := new(map[string]interface{})
   t.Log(json.Unmarshal([]byte(`{"error":0}`), m))
	if code, ok := (*m)["error"].(int); !ok || code != 0 {
		t.Log(ok)
		t.Log(code)
	}

}
```

### 问题1 ：这段程序会打印出什么？

结果：

```
false
0
```

### 问题2 ：这段程序有哪些点需要注意？

1. `new()`函数返回了什么? m 具体是什么？

   答: `new()`函数返回了，指向指定类型零值的指针。m 是一个指针，这个指针不为空。但它指向一个零值的`map`。*见 代码1 引出的问题*

2. 疑问：除了 `new()` 函数，有没有其他方法创造出上述 `m`这样的 `map`?

   答: 目前通过正常途径无法做到。

3. 疑问：`Unmarshal`为什么可以正常处理这样的 `map`？

   答：要将JSON对象`Unmarshal`到映射中，`Unmarshal`首先要建立要使用的`map`。如果`map`为零值，`Unmarshal`将分配一个新`map`。否则，`Unmarshal`会重用现有地图，并保留现有条目。然后，`Unmarshal`将来自JSON对象的键/值对存储到映射中。`map`的键类型必须是字符串，整数或实现`encoding.TextUnmarshaler`。

---



5. 疑问：为什么 ok == false ?

答：`error` 值实际类型为 `float64`，运行如下代码。`Unmarshal` 将数字 `0` 解析为 `float64` 类型：

```go
	m := new(map[string]interface{})
	t.Log(json.Unmarshal([]byte(`{"dm_error":0}`), m))
	if code, ok := (*m)["dm_error"]; !ok || code != 0 {
		t.Log(ok)
		t.Log(reflect.TypeOf(code))
	}
```

6. `if code, ok := (*m)["dm_error"]; !ok || code != 0 {`  这行中 `code` 作为 `interface{}`类型可以直接与具体类型的值比？

   答：当然可以这么做，只是平时的编码习惯将其断言为具体类型。但是需要注意`interface{}`的具体类型是什么,就像上面遇到的问题那样？

