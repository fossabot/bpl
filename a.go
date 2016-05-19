package main

import "fmt"

func main() {
	var i uint8 = 255
	var j uint8 = 255 + 1 + i + 1
	fmt.Printf("%#v, %#v\n", i, j)
}
