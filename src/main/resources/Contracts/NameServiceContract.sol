pragma solidity ^0.4.0;

import './Pausable.sol';


contract NameServiceContract is Pausable {

    string[] public names;
    mapping(bytes32 => uint) private indexOfNameHash; // Incremented indexes
    mapping(bytes32 => address) private addressOfNameHash;

    function incrementedIndexOfName(string name) public constant returns(uint) {
        return indexOfNameHash[keccak256(name)];
    }

    function addressOfName(string name) public constant returns(address) {
        return addressOfNameHash[keccak256(name)];
    }

    function setAddressOf(string name, address addr) public onlyOwner whenNotPaused {
        bytes32 nameHash = keccak256(name);
        if (indexOfNameHash[nameHash] == 0) {
            names.push(name);
            indexOfNameHash[nameHash] = names.length; // Incremented
        }
        addressOfNameHash[nameHash] = addr;
    }

}
