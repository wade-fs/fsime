package main
 
import (
    "bufio"
    "fmt"
    "os"
	"strings"
)
 
func main() {
	if len(os.Args) == 1 {
		fmt.Printf("Usage: %s FILE\n", os.Args[0])
		return
	}
    readFile, err := os.Open(os.Args[1])
  
    if err != nil {
        fmt.Println(err)
		return
    }
	defer readFile.Close()

    fileScanner := bufio.NewScanner(readFile)
    fileScanner.Split(bufio.ScanLines)
  
    for fileScanner.Scan() {
        line := fileScanner.Text()
		res := strings.SplitAfter(line, "")
		if len(res) == 2 {
			fmt.Println(line)
			continue
		}
		for i:=0; i<len(res)-1; i++ {
			fmt.Printf("%s%s\n", res[i], res[i+1])
		}
    }

    readFile.Close()
}
