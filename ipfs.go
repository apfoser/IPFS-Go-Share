package main

/*

	TODO:
	1. Automatically set port number instead of assuming "localhost:5001"
	2. Keeping filetype information
		- Store whole directory || ask user for file extension -> DONE
		- Stems from "Content-Hashing"
	3. Storing the files -> give user ability to pick where to store
	4. Add whole files to IPFS -> DONE

*/

import(
	"fmt"
	//"errors"
	//"strings"
	//"io"
	"log"
  "os"
	"bufio"
	// ipfs go api
	"github.com/ipfs/go-ipfs-api"
	// attaching variables to keyboard
	"github.com/atotto/clipboard"
)

func main(){

	status := true

	for status != false {

		var choice string
		fmt.Printf("Press 1 to add to network, 2 to retrieve from network, 3 to quit: ")
		fmt.Scanf("%s", &choice)

		if choice == "1" {
			add()
		} else if choice == "2"{
			get()
		} else {
			status = false
		}
	}
}

/*
Add() the file found in var path to the IPFS network. Assumes daemon port is at localhost:5001
*/

func add() {

	fmt.Print("Enter file path: ")
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Scan()
	path := scanner.Text()

	file, err := os.Open(path)

	if err != nil {
		fmt.Println(err.Error())
	}

	sh := shell.NewShell("localhost:5001")
	cid, err := sh.Add(file)
	if err != nil {
        fmt.Fprintf(os.Stderr, "error: %s", err)
        os.Exit(1)
	}

	clipboard.WriteAll(cid)
  fmt.Println("Added to IPFS! CID:", cid)

	return
}

/*
Get() function is misnamed. Does ipfs cat hash and ipfs get hash.
Need to be split up into two different functions
*/

func get() {

	var hash string

	fmt.Print("Enter your hash: ")
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Scan()
	hash = scanner.Text()

	sh := shell.NewShell("localhost:5001")

	var download string
	fmt.Printf("Would you like to download this file? y/n: ")
	fmt.Scanf("%s", &download)

	if download == "y" {

		var name string
		fmt.Printf("Specify a filename, including the file extension: ")
		fmt.Scanf("%s", &name)
		err := sh.Get(hash, name)

		if err != nil {
			log.Fatal(err)
		}

	} else{
		return
	}
	return
}
