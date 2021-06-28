import "./IERC20.sol";

abstract contract BabyKrakenToken is IERC20  {
    uint256 public _maxTxAmount;
    address public uniswapV2Pair;

    // uint256 public sellLimit;
    // bool public tradingEnabled;
    // function getSellLockTimeInSeconds() public virtual view returns(uint256);
}
