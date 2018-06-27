pragma solidity ^0.4.0;


contract IStorageContractClient {

    uint256 public storageIdentifier;

}


contract StorageContract {

    mapping (uint256 => uint256) public dict;

    function set(uint256 key, uint256 value) public {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        dict[uint256(keccak256(id, key))] = value;
    }

    function erase(uint256 key) public {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        delete dict[uint256(keccak256(id, key))];
    }

    function get(uint256 key) public constant returns(uint256) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        return dict[uint256(keccak256(id, key))];
    }

    function length(uint256 key) public constant returns(uint) {
        uint256 id = IStorageContractClient(msg.sender).storageIdentifier();
        uint len = 0;
        while (dict[uint256(keccak256(id, key + len))] != 0) {
            len++;
        }
        return len;
    }

}
